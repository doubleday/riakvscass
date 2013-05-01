COMMAND='cat /var/log/wooga/rails-production.log' cap invoke app > rails-production.log 2>&1
COMMAND='cat /var/log/nginx/g9-live.wooga.com_lb.access.log' cap invoke app01 app02 > nginx.log 2>&1
