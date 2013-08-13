# zoo-live

An application to display all of Zooniverse's classifications on a map. 

## Prerequisites

You will need [Leiningen][1] 1.7.0 or above installed and [redis][2]

On OS X with [homebrew][3] installed you can run `brew install leiningen redis`.

[1]: https://github.com/technomancy/leiningen
[2]: http://redis.io
[3]: https://github.com/mxcl/homebrew 

## Usage

You can run either from the repl or on the commandline:

    lein repl
    (go PORT)

or
    
    lein run -m zoo-live.system PORT

You can specify the port it will run on and the URIs for both the main redis connection and the redis pubsub connection as environment variables, as `PORT`, `REDIS_PUB_SUB`, `REDIS`. By default it will use port 8080 and a local redis instance at `redis://127.0.0.1:6379/0`.

## License

Copyright © 2013 Zooniverse. This software is available under the APL see COPYING.
