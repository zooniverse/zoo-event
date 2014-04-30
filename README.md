# zoo-event

HTTP front end for Zooniverse Event log and the Zooniverse Kafka event stream

## Prerequisites

You will need [Leiningen][1] 2.0.0 or above installed. 

On OS X with [homebrew][3] installed you can run `brew install leiningen`.

[1]: https://github.com/technomancy/leiningen
[2]: http://redis.io
[3]: https://github.com/mxcl/homebrew 

## Usage

Run from the commandline passing an [edn](https://github.com/edn-format/edn) formated config file. 
    
    lein run -m zoo-live.system conf.edn

The config file should be in the format:

    {:postgres <jdbc-connection>
     :zookeeper <comma,seperated,zookeeper,uris>}

## License

Copyright © 2013,2014 Zooniverse. This software is available under the APL see COPYING.

