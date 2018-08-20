日本語のREADMEは [こちら](https://github.com/SlashNephy/Tweetstorm/blob/master/README.md) です.  
English README is updaing now.  

# Tweetstorm
A simple substitute implementation for the Twitter UserStream.

## What is this?
Tweetstorm aims to simulate the UserStream API of Twitter which retired on August 23th, 2018.  
Tweetstorm provides following json data.
- Tweet (Status)  
- Direct messages  
- Event (WIP, pushes both of about_me & by_friends.)  
- Friends  
- Delete (WIP, only if Account Activity API is available.)

But following data type is **NOT** provided.
- Status Withheld (There is no longer way to get.)
- Limit (Same.)
- Some events? (I'm investigating now.)
