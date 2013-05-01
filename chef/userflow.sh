grep $1 nginx.log | cut -d ] -f 2,3 | sort | less
