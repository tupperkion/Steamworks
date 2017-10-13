# `WebSocketTable`

WebSocketTables are similar to the WPILib NetworkTables, in that they allow a synchronized table to be stored between multiple devices. They work differently at a fundamental level though, which improves compatibility and performance in many cases.

WebSocketTables run by using WebSockets as a pipeline, which allows for easy compability with JavaScript. Unlike with NetworkTables, updates are sent as soon as a change is made in a table, ensuring minimal latency. WebSocketTables currently support Strings, doubles, and boolean values.

The contents of the table can be modified by a call to `setString(String name, String value)`, `setDouble(String name, double value)`, and `setBoolean(String name, boolean value)`, and retrieved using `getString(String name)` and `Double` and `Boolean` variants of that method. These methods should be self-explanatory.

A call to the `synchronize()` method will synchronize the table across all parties involved with it's own. If this is called on a server, the table will be sent to all clients, and all clients should update their contents to match. If this is called on a client, the table will be sent to the server, which will adopt the changes and forward the table to all other clients. This should not be necessary, however, as smaller update messages are sent every time a `set...` method is called which should keep the table synchronized.

## The Server

Servers are currently supported in Java, and based on the Java [`WebSocketTableServer`](src/main/java/com/midcoastmaineiacs/Steamworks/WebSocketTableServer.java) class. To create a server, create a new class which extends `WebSocketTableServer` and use the `super` constructor to specify parameters. A server is intialized with up to three parameters. A String, `name`, specifies the name of the server, which is prepended before all log messages. This is the only way the name value is currently used. An int, port, specifies what port the server is listening under. Clients will need to know this port to connect. A boolean value (optional, defaults to `false`) defines whether or not the server is recessive. More on that later.

When a server is first created, the constructor will call the `setDefaults()` method, which must be overriden by subclasses. Overrides of this method should use the `set...` methods to specify the default values of the table. Note that the table will not attempt to synchronize these changes, as the server has not been started yet. When this function returns, the server will start accepting connections and synchronizing all future changes.

## Networking

Whenever a client connects to a server, the server may decide whether or not it is recessive. At the time of writing, this is simpl specified by the constructor. If it is not recessive, it will call its own `synchronize` method, pushing the current state of the table on the server to the clients. If it is recessive, it will send a `pull` message to the clients. The client may either A) send its data (or defaults) to the server to synchronize it, or B) force the server to synchronize by sending its own `pull` method.

From this point on, any changes made to the table via the `set...` methods will send a `change` message to the other side. The other side should then adopt the changes, and if it's a server, forward the change to all other clients. Keys can be any valid string, as can string values.

## Implementation

### Server

Whenever a client connects to a server, it can either send `sync:` followed by the serialized table, or send `pull` through the WebSocket. Whenever a server receives a message starting with `sync:`, it should unserialize the remaining part of the message, import the values into its table, then forward the message to all clients, except the one that sent it the `sync:` message.

Whenever a change is made to a server's table, it must send `change:` followed by the serialized value of the table (_not the whole table_) to all clients. When it receives a message starting with `change:`, it must unserialize the remaining part of the message, import the value into its table, then forward the message to all clients, except the one that sent it the `change:` message.

Whenever a server recieves a `pull` message, it should synchronize by sending a `sync:` to _all clients_ as if they had just connected as new clients.

### Client

The client is implemented very similarly to the server, with some key differences. When connecting to a server, it does not send the first message, it should wait for a message from the server and obey what that message says to do. (e.g. unserialize data or send its own data). If the client wants to be recessive, even to a server that is supposed to be recessive, it may reply to the first `pull` with another `pull` rather than synchronizing. Also, the client does not need to worry about forwarding messages it receives.

The client should also continously attempt to connect to a server, and attempt to reconnect again if a connection attempt fails or the connection is closed.

## Serialization

Data is transmitted as strings through the WebSocket. Serialization methods are used to convert the data to and from a string.

### Serializing a single value

To serialize a value, start off with a string containing the length of the key. Then append a `;` and the key. After that, append a single character indicating the type of data. This can be `B`, `D`, or `S`, for a boolean, double, or string respectively. Finally, append the data itself. Doubles must be serialized in a decimal format, and booleans must be represented as either `"true"` or `"false"`. For example, a boolean value of `false` named `"toggle"` becomes `6;toggleBfalse`.

### Unserializing a single value

Split the string in half by the first `;`. This will give you the key length as one string (immediately parse it as an int), and the data in another string. Using the length integer, extract the key from the beginning of the string. Then use the letter to figure out what type to use, and extract the rest of the data.

### Serializing the table

To serialize the whole table, iterate over the entire table. For each key, append the same string you would get from serializing a single value with one big difference. Before the letter which indicates the value, append the length of the value (including the letter indicating the type), and a semicolon.

> See [WebSocketTableServer.java](src/main/java/com/midcoastmaineiacs/Steamworks/WebSocketTableServer.java) for an example implementation that may simplify this explanation.
> Note that the `serialize` method in that file only serialize the value and not the key. Refer to the `setValue` code to see how to properly serialize a key and value.
