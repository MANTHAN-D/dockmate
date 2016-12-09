FROM ubuntu

MAINTAINER RENU BATHEJA

# Installation:
# Install haproxy and required utils

RUN apt-get update && \
   apt-get install -y curl unzip haproxy iptables apache2-utils

# Install consul-template

RUN curl -L -o /tmp/consul-template.zip https://releases.hashicorp.com/consul-template/0.16.0/consul-template_0.16.0_linux_amd64.zip && \
   cd /tmp && \
   unzip consul-template* && \
   cp /tmp/consul-template /usr/local/bin/consul-template && \
   chmod a+x /usr/local/bin/consul-template \
   && apt-get remove -y curl unzip \
   && apt-get autoremove -y \
   && apt-get autoclean

# Set the dockerized entry-point

ENTRYPOINT ["/bin/sh"]
CMD ["/start.sh"]

# Copy required artifacts to haproxy service container

ADD haproxy.conf /etc/haproxy/haproxy.cfg
ADD start.sh /start.sh
RUN chmod u+x /start.sh
ADD reload_haproxy.sh /reload_haproxy.sh
RUN chmod u+x /reload_haproxy.sh

