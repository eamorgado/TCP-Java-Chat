# A Simple TCP user-server chat

## Introduction

This project consists in the development of a simple chat server in order for a client to communicate with it.

The server will be based on the multiplex model.

## Command Line
The server will be implemented in a ChatServer.java file (with auxiliary classes) and will accept as a command-line argument the TCP port where it will listen for actions.

For example:
```bash
java  ChatServer 8000
```

The client is in a ChatClient.java file (with auxiliary classes) and will accept as command-line arguments the DNS name of the server as well as the TCP port where the server is running.

For example:
```bash
java ChatClient localhost 8000
```


## Protocol
The communication protocol is a text-based protocol, meaning that every message between clients and the server ends with a new line, as such, any message that a client wants to send cannot have a new line, as that represents the end of the user's command.

The user can also send simple commands/messages to the server. These commands are in the format:
- /command

A command can have arguments, separated by spaces. Simple messages can only be sent when the user is inside a lobby.

The server supports the following commands:

```diff
- /nick name
  Gives the user its username.
  If the user already has a username, updates' it.
  If name is already in use it returns an error message.

- /join lobby
  Command used to enter a lobby.
  It can only be used if the user already has a username.
  If the lobby does not exist, it creates it.
  If the user is inside another lobby, leaves that lobby and joins the new one.
  Informs other users in the necessary lobbies that a user has left/joined the lobby.

- /leave
  Command used to exit from a lobby, if the user isn't in any lobby returns and error.
  Informs all the users inside the lobby that a user has left their lobby.

- /bye
  This command terminates a user's chat session, exiting the user from any lobby.

- /priv [user] [message]
  This commands will, if [user] exists and the current user has a valid username, send a private/direct message to [user].
  This direct message is independent of lobbies, meaning that a user doesn't need to be in a lobby to send them.

```


The server sends messages to the users. They always start in a capital word which indicates the type of message, the message may include arguments depending on its type.

The server can send the following messages

```diff
+ OK
  This message indicates the success of the command sent by the user.

- ERROR
  This message indicates that the command sent by the user was not successful.

+ MESSAGE [user] [message]
  This command is used to inform the users in a lobby that [user] has sent a message with the contents of [message]

+ PRIVATE [user] [message]
  Tells the current user that a user of username [user] has sent a message with contents [message]

+ NEWNICK [old] [new]
  This message indicates to all users inside the current user's lobby that he has changed his username.

+ JOINED [user]
  This message indicates to the members of the relevant lobby that a new user has entered.

+ LEFT [user]
  Indicates to the members of the relevant lobby that the user [user] has left the lobby.

+ BYE
  Tells the current user that the server will initiate the termination of its session in the chat.

```


## What you can see
This program will display messages in the terminal.

However, you will be given, once you execute the ChatClient, a GUI in order to facilitate the whole process of reading messages and server responses. 

In this GUI there will be 4 sections, a bottom one where you will be able to type the commands/messages, one main section on the left, where all lobby messages and lobby-server related responses will be presented, two sections on the right, one above the other, where the first (the upper section) will display all private/direct messages to the current user and the second section (the bottom one) will only display the server responses (the OK's and the ERROR's).


## Where can you get this
This file, as well as all the chat files, can be found in my repository [eamorgado/TCP-Java-Chat](https://github.com/eamorgado/TCP-Java-Chat).

