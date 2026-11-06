package com.example.eac_backend_project.transport;
import java.time.*;
import java.time.temporal.ChronoUnit;
import java.math.*;
import java.util.*;
import static com.example.eac_backend_project.transport.TransportValues.*;

final class TransportForecast {
 private TransportForecast(){}
 static Map<String,Object> calculate(TransportComponent component,Double currentMileage,List<TransportOdometer> history,LocalDate today,int minReadings,int maxAgeDays){
  return calculate(component,currentMileage,history,today,minReadings,maxAgeDays,List.of(1000,500,0),List.of(30,7,0));
 }
 static Map<String,Object> calculate(TransportComponent component,Double currentMileage,List<TransportOdometer> history,LocalDate today,int minReadings,int maxAgeDays,List<Integer> kmThresholds,List<Integer> dayThresholds){
  List<TransportOdometer> ordered=new ArrayList<>(history.stream().filter(r->r.readingDate!=null&&r.reading!=null&&r.supersededAt==null).toList());ordered.sort(Comparator.comparing((TransportOdometer r)->r.readingDate).thenComparing(r->r.createdAt));
  int lastCorrection=-1;for(int i=0;i<ordered.size();i++)if(ordered.get(i).correction)lastCorrection=i;
  if(lastCorrection>=0)ordered=new ArrayList<>(ordered.subList(lastCorrection,ordered.size()));
  Map<LocalDate,TransportOdometer> daily=new TreeMap<>();for(var row:ordered)if(row.readingDate!=null&&row.reading!=null&&!row.readingDate.isAfter(today))daily.put(row.readingDate,row);
  List<TransportOdometer> valid=new ArrayList<>(daily.values());BigDecimal current=decimal(currentMileage);BigDecimal remaining=current==null||component.nextServiceMileage==null?null:component.nextServiceMileage.subtract(current);Long days=component.nextServiceDate==null?null:ChronoUnit.DAYS.between(today,component.nextServiceDate);
  LocalDate asOf=valid.isEmpty()?null:valid.get(valid.size()-1).readingDate;boolean stale=asOf==null||ChronoUnit.DAYS.between(asOf,today)>maxAgeDays;
  BigDecimal average=null;LocalDate predicted=null;String explanation="At least "+minReadings+" valid readings on different dates are required.";
  if(valid.size()>=minReadings){var first=valid.get(0);var last=valid.get(valid.size()-1);long span=ChronoUnit.DAYS.between(first.readingDate,last.readingDate);BigDecimal distance=last.reading.subtract(first.reading);if(span>0&&distance.signum()>0){average=distance.divide(BigDecimal.valueOf(span),3,RoundingMode.HALF_UP);if(!stale&&remaining!=null&&remaining.signum()>0&&current!=null&&current.compareTo(last.reading)==0){long estimatedDays=remaining.divide(average,0,RoundingMode.CEILING).longValue();if(estimatedDays<=36500)predicted=last.readingDate.plusDays(estimatedDays);explanation=remaining.toPlainString()+" km remaining ÷ "+average.toPlainString()+" km/day, measured across "+span+" days. Estimate starts from "+last.readingDate+".";}else explanation=stale?"Mileage history is stale; record a fresh odometer reading before forecasting.":remaining==null?"Current mileage or the next mileage threshold is not recorded.":"Mileage is due or does not match the latest dated reading; no estimated date is invented.";}}
  int soonDays=component.warningDays==null?dayThresholds.stream().filter(n->n>0).min(Integer::compareTo).orElse(0):component.warningDays;BigDecimal soonKm=component.warningKm==null?BigDecimal.valueOf(kmThresholds.stream().filter(n->n>0).min(Integer::compareTo).orElse(0)):component.warningKm;int upcomingDays=Math.max(soonDays,dayThresholds.stream().max(Integer::compareTo).orElse(0));BigDecimal upcomingKm=soonKm.max(BigDecimal.valueOf(kmThresholds.stream().max(Integer::compareTo).orElse(0)));
  String state="NOT_RECORDED";if(days!=null&&days<0||remaining!=null&&remaining.signum()<0)state="OVERDUE";else if(days!=null&&days==0||remaining!=null&&remaining.signum()==0)state="DUE";else if(days!=null&&days<=soonDays||remaining!=null&&remaining.compareTo(soonKm)<=0)state="DUE_SOON";else if(days!=null&&days<=upcomingDays||remaining!=null&&remaining.compareTo(upcomingKm)<=0)state="UPCOMING";else if(days!=null||remaining!=null)state="SCHEDULED";
  return map("status",state,"remainingKm",remaining,"daysRemaining",days,"averageDailyKm",average,"predictedServiceDate",predicted,"mileageAsOf",asOf,"dataStale",stale,"readingCount",valid.size(),"explanation",explanation,"nextServiceDate",component.nextServiceDate,"nextServiceMileage",component.nextServiceMileage);
 }
}
