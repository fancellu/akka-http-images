#akka-http-images

Serves up an image on the /image endpoint, with the left hand side distorted pixel by pixel,
by some random amount to each r,g,b value

Each request will return a different image

With current distortion offset of max 5, this will be a subtle effect, but visible

Along with suitable unit tests to prove that this is being done

`sbt run` to run up

`sbt test` to run the tests

Written to be as simple as possible, with few moving parts i.e. 3 main files