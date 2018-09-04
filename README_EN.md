日本語のREADMEは [こちら](https://github.com/SlashNephy/Tweetstorm/blob/master/README.md) です.  

# Tweetstorm
A simple substitute implementation for the Twitter UserStream.  
Tweetstorm's goal is to simulate the UserStream API which retired on August 23th, 2018 as possible. 

```
                 User Stream API                                          Twitter API
             +--------------------+     +-------+     +------------+     +------------+
 Client A -> | GET /1.1/user.json |     |       |     |            | <-> |   Tweets   |
             +--------------------+     |       |     |            |     +------------+
             +--------------------+     |       |     |            | <-> | Activities |
 Client B -> | GET /1.1/user.json | <-> | nginx | <-> | Tweetstorm |     +------------+
             +--------------------+     |       |     |            | <-> |   Friends  |
             +--------------------+     |       |     |            |     +------------+
 Client C -> | GET /1.1/user.json |     |       |     |            | <-> |    etc...  |
             +--------------------+     +-------+     +------------+     +------------+
          ~        userstream.twitter.com          ~  127.0.0.1:8080 (Default)
```

Tweetstorm provides following json data.
- Tweets  
- Direct messages    
- Friend Ids  

Also, next following json data type is NOT provided yet. But future update may make it possible.
- Activities / Events

## Wiki
Sections like Setup or Compatibility are moved to [Wiki](https://github.com/SlashNephy/Tweetstorm/wiki).

## License
This project is provided under the MIT license.


