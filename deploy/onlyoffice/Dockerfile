FROM onlyoffice/documentserver:10.0.0@sha256:ca17889f10b11141cc6441836aebdd2835cf3e1b49681b2fd6e155c94b7850b3
COPY deploy/onlyoffice/sheet-access/ /var/www/onlyoffice/documentserver/sdkjs-plugins/{B785C60D-E5F1-48D1-A624-2BF8EAC50001}/
RUN find '/var/www/onlyoffice/documentserver/sdkjs-plugins/{B785C60D-E5F1-48D1-A624-2BF8EAC50001}' -type f \( -name '*.js' -o -name '*.json' -o -name '*.html' -o -name '*.css' -o -name '*.svg' \) -exec gzip -kf {} \;
COPY deploy/onlyoffice/synology-entrypoint.sh /usr/local/bin/eac-office-entrypoint.sh
RUN sed -i 's/\r$//' /usr/local/bin/eac-office-entrypoint.sh && chmod 755 /usr/local/bin/eac-office-entrypoint.sh
ENTRYPOINT ["/usr/local/bin/eac-office-entrypoint.sh"]
