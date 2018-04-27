# VertxSimpleWebChat

## What do I need?

* A text editor or an IDE (Intellij will be used, Atom or sublime will work to)
* Java 8 (Oracle JDK will be used, OpenJDK should work to)
* A web browser
* Node and Npm for static files resolution
* A command line (should also work on windows)

> In case you're using Sublime or Atom, would be nice to have the gradle and groovy plugin installed

------

# Index <a name="index"></a> 

* [Setting up web server](#link-1)
* [Working with npm and static files](#link-2)
* [Having fun with the event bus](#link-3)

### Setting up web server <sub><sub><sub>[Index](#index)</sub></sub></sub> <a name="link-1"></a>
------

Import the required packages in the project, just below the package definition.

```groovy
import io.vertx.core.http.HttpServer
import io.vertx.ext.web.Router
import io.vertx.ext.web.handler.StaticHandler
```


Place the next code inside the SimpleChat class, located at `src/main/groovy/mx/jresendiz/SimpleChat.groovy` inside the class definition.

```groovy
HttpServer server;
Router router;

public void start() {
    // This method is called whenever you create a verticle
    server = vertx.createHttpServer()
    router = Router.router(vertx)
    router.route().handler(StaticHandler.create())
    server.requestHandler(router.&accept).listen(8080)
}

public void stop() {
    // This method is called whenever a verticle is closed or dies
    log.info("Verticle has been closed!")
}
```

Then run the project using `./gradlew clean run`

### Working with npm and static files <sub><sub><sub>[Index](#index)</sub></sub></sub> <a name="link-2"></a>
------

* Install Vert.x dependency in the project.

```bash
$ ./gradlew npmInstall
```

> This will install all the required libraries for the project

* Modify _index.html_, add bootstrap, jquery and vert.x libraries. Add the next code inside the ```<head></head>``` tag.

```html
<script src="//cdn.jsdelivr.net/sockjs/1/sockjs.min.js"></script>
<script src="https://code.jquery.com/jquery-1.11.2.min.js"></script>
<link href="https://maxcdn.bootstrapcdn.com/bootstrap/3.3.7/css/bootstrap.min.css" rel="stylesheet" crossorigin="anonymous">
<script src="node_modules/vertx3-eventbus-client/vertx-eventbus.js"></script>

```
* Also add the next content inside the ```<body>``` tag.

```html
<nav class="navbar navbar-default">
    <div class="container-fluid">
        <div class="navbar-header">
            <a class="navbar-brand" href="#">
                <img alt="Brand" src="http://vertx.io/vertx2/logo-white-big.png" width="55px" height="20px">
            </a>
        </div>
        <div class="collapse navbar-collapse">
            <form class="navbar-form navbar-nav" role="search">
                <div class="form-group">
                    <input type="text" id="username" class="form-control" disabled>
                    <input type="text" id="content" class="form-control" placeholder="What do you wanna say?">
                </div>
                <button id="send" class="btn btn-default">Send</button>
                <button id="clear" class="btn btn-danger">Clear</button>
            </form>
        </div>
    </div>
</nav>
<div class="container">
    <div class="row">
        <div class="col-md-2 col-md-offset-5">
            <strong>Current live...</strong>
        </div>
        <div id="wall" class="col-md-6 col-md-offset-3">

        </div>
    </div>
</div>
```

* All the changes now should appear at [http://localhost:8080](http://localhost:8080) 

------
### Having fun with the event bus <a name="link-3"></a> <sub><sub><sub>[Index](#index)</sub></sub></sub>
------

* We need to import ```vertx-eventbus-js``` into our project, consider paste the next line at the end of the body tag.

```html
<script src="node_modules/vertx3-eventbus-client/vertx-eventbus.js"></script>
``` 

* Vert.x uses handlers and events, we need to create the basic handlers for our chat application, this can be on a new javascript file, or inside a ```<script>``` tag at the end of the body. This script will create a new socket connection, and all the required handlers. 

```html
<script>
    $(document).ready(function () {
        if (!localStorage.getItem("username")) {
            localStorage.setItem("username", window.navigator.appCodeName + "-" + parseInt(Math.random() * 100));
        }
        $("#username").val(localStorage.getItem("username"));
    });
    var eventBus = new EventBus("http://localhost:8080/chat");
    function drawMessage(message) {
        var content = "<div class='row'>" +
            "<div class=\"btn-group btn-group-xs\" role=\"group\" aria-label=\"...\" style=\"width:100% !important;\">" +
            "<button type=\"button\" class=\"btn btn-default\" style=\"width:20% !important;\">" + message.body.username + "</button>" +
            "<button type=\"button\" class=\"btn btn-default\" style=\"width:80% !important;\">" + message.body.content + "</button>" +
            "</div>" +
            "</div>";
        $("#wall").append(content);
    }

    eventBus.onopen = function () {
        eventBus.registerHandler("newMessage", function (err, message) {
            drawMessage(message);
            window.scrollTo(0, document.body.scrollHeight);
        });
    };
    $("#clear").on("click", function (event) {
        event.preventDefault();
        $("#wall").html("");
        window.scrollTo(0, 0);
    });
    $("#send").on("keyup", function (event) {
        if (event.keyCode == 13 || event.which == 13) {
            send();
            $('#content').val("");
        }
    });
    $("#send").on("click", function (event) {
        event.preventDefault();
        send();
        $('#content').val("");
    });
    function send() {
        var currentMessage = {};
        currentMessage.username = localStorage.getItem("username") || "anonymous";
        currentMessage.content = $("#content").val() || " -- -- -- -- -- --";
        eventBus.send("sendMessage", currentMessage);
        drawMessage(currentMessage);
        $('#content').val("");
    }
</script>
```

* Now, on the back-end, we have to configure the server and create a *bridge* with the event bus. First, we need to update the imports. Just below the package, paste the new classes to be imported.


```groovy
import io.vertx.core.eventbus.EventBus
import io.vertx.core.json.JsonObject
import io.vertx.ext.bridge.BridgeEventType
import io.vertx.ext.web.handler.sockjs.SockJSHandler
```
 
 * We will have fun with the event bus. Consider the next code, this should be pasted just bellow the ``` class``` declaration (As instance variables).
  
```groovy
HttpServer server;
Router router;
EventBus eventBus;
final Integer VERTX_PORT = (System.getenv("PORT") ? System.getenv("PORT") : "8080") as Integer
```
 
* The body of the ```start()``` method must be updated. First of all we have to access the eventbus and create the restrictions of it. This must be the new body.
 
```groovy
// This method is called whenever you create a verticle
server = vertx.createHttpServer()
eventBus = vertx.eventBus();
// This options define the allowed addresses, in and out
def options = [
        inboundPermitteds : [
                [address: "sendMessage"],
        ],
        outboundPermitteds: [
                [address: "newMessage"],
        ],
        heartbeatInterval : 2000
]
// Define the handler for the websockets
SockJSHandler sockJSHandler = SockJSHandler.create(vertx).bridge(options, { eventHandler ->
    if (eventHandler.type() == BridgeEventType.SOCKET_CLOSED || eventHandler.type() == BridgeEventType.SOCKET_CREATED) {
        eventBus.publish("newMessage", [username: "Bot", content: "Siento un disturbio en la fuerza..."])
    }
    eventHandler.complete(true);
});

router = Router.router(vertx)
// Define a channel named chat, this is where the EventBus will live
router.route("/chat/*").handler(sockJSHandler)
router.route().handler(StaticHandler.create())

server.requestHandler(router.&accept).listen(VERTX_PORT, "0.0.0.0")
// Register handlers to event bus
eventBus.consumer("sendMessage").handler({ message ->
    JsonObject messageBody = (JsonObject) message.body();
    eventBus.publish("newMessage", messageBody);
});

log.info("Verticle has started!!")
```

* Now we can take a look to [http://localhost:8080](http://localhost:8080/) and see what's happening

> Don't forget to set your environment variable called _LOCAL_IP_. Consider the next command and update the value `export LOCAL_IP=YOUR_LOCAL_IP`.
> If you can't see the page, try killing the ```./gradlew run``` and run the application again.

----------
