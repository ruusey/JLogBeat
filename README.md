## JLogBeat

Tired of dealing with ELK and absurd winlogbeat indeces? Introducing `JLogBeat`
* Requires Java 11
* Requires Maven 3.8 or later.

This program collects windows System, Application Security and Firewall logs asynchronously and stores them in a normalized fashion within MYSQL allowing for easy querying.

## WinEvent Logs:

Currently supported: 
 * [GET] /event - Get paginated event history
 * [GET] /event/{id} - Get Event Log by Its DB ID
 * [GET] /event/source/{source} -Get paginated event history by source (application, security, system)
 
## Firewall Logs:

Currently supported: 
 * [GET] /event/firewall - Get paginated event history for the Firewall Log
 * [GET] /event/firewall/{query} -Get paginated event history for the Firewall Log based on query. Query may be a local Ip, A destination IP or a protocol

## In Action:

![alt text](https://i.imgur.com/rqIPEOo.png)
![alt text](https://i.imgur.com/kchnGxx.png)
![alt text](https://i.imgur.com/TBKgAsU.png)
![alt text](https://i.imgur.com/O2dtkbt.png)

