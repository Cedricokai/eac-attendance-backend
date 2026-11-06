package com.example.eac_backend_project.files;
import org.springframework.web.bind.annotation.*;
@RestController @RequestMapping("/api/files")
public class FileWorkReviewController {
 private final FileWorkReviewService review;
 public FileWorkReviewController(FileWorkReviewService review){this.review=review;}
 public record ReturnRequest(long version,String note){}
 @PostMapping("/{id}/return-work") public Object send(@PathVariable Long id,@RequestBody ReturnRequest request){return review.sendBack(id,request.version(),request.note());}
 @GetMapping("/work-review/returns") public Object inbox(@RequestParam(defaultValue="0")int page){return review.inbox(page);}
 @GetMapping("/work-review/sent") public Object sent(@RequestParam(defaultValue="0")int page){return review.sent(page);}
 @PutMapping("/work-review/returns/{id}/reviewed") public Object reviewed(@PathVariable Long id){return review.reviewed(id);}
}
