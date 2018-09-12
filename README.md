[![Kotlin 1.2.61](https://img.shields.io/badge/Kotlin-1.2.61-blue.svg)](http://kotlinlang.org)
[![Licence](https://img.shields.io/github/license/SlashNephy/Tweetstorm.svg)](https://github.com/SlashNephy/Tweetstorm/blob/master/LICENSE)
[![GitHub release](https://img.shields.io/github/release/SlashNephy/Tweetstorm.svg)](https://github.com/SlashNephy/Tweetstorm/releases)
[![Docker Build Status](https://shields.beevelop.com/docker/pulls/slashnephy/tweetstorm.svg)](https://hub.docker.com/r/slashnephy/tweetstorm)
[![Github All Releases](https://img.shields.io/github/downloads/SlashNephy/TweetStorm/total.svg)](https://github.com/SlashNephy/Tweetstorm/releases)
[![GitHub issues](https://img.shields.io/github/issues/SlashNephy/Tweetstorm.svg)](https://github.com/SlashNephy/Tweetstorm/issues)
[![GitHub pull requests](https://img.shields.io/github/issues-pr/SlashNephy/Tweetstorm.svg)](https://github.com/SlashNephy/Tweetstorm/pulls)

English README is [here](https://github.com/SlashNephy/Tweetstorm/blob/master/README_EN.md).  

# Tweetstorm: UserStream APIの簡単な代替実装  
Tweetstormはクライアントに代わってREST APIを呼び出し, 従来のTwitter UserStream APIと同等のインターフェイスで配信します。  
Tweetstormは2018/8/23に完全に廃止されたUserStreamをできる限り再現することを目標としています。

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
 
Tweetstormは次のJSONデータを提供します。  
- ツイート  
  - `list_id`でリストを指定したかどうかや, そのリストに自分のアカウントが含まれているかどうかで挙動が異なります。  
  
    |`list_id`を指定した?|そのリストに自分が含まれている?|取得されるタイムライン|
    |:--:|:--:|--:|
    |Yes|Yes|List|
    |Yes|No|List, User, Mentions|
    |No|-|Home, User, Mentions|
    
    なお, Tweetstormにはリストにフォローユーザを同期する機能があります。これはデフォルトでは無効ですが, `sync_list_following`で切り替えることで有効にできます。
    
  - タイムラインによってレートリミットが異なります。そのため取得間隔に違いがあります。  
  
    |タイムライン|API レートリミット|Tweetstormでのデフォルトの取得間隔|
    |:--:|:--:|--:|
    |Home|15回/15分|90秒|
    |List|900回/15分|3秒|
    |User|900回/15分|3秒|
    |Mentions|75回/15分|45秒| 

- ダイレクトメッセージ  
  - デフォルトで有効ですが, `enable_direct_message`で切り替えできます。
  - レートリミットは 15回/15分 で, デフォルトの取得間隔は 90秒 です。
- アクティビティ  
  - デフォルトでは無効です。有効にするには `enable_activity`で切り替え, `Twitter for iPhone`のアクセストークンを設定する必要があります。
  - 自分が関係しているイベントかつポジティブなイベントだけ配信されます。
    - 例えば, 自分のツイートに対するお気に入りイベントは得られますが, お気に入り解除イベントや, 他人による他人のツイートへのお気に入りイベントを得ることはできません。
    - この制約のため, 従来のUserStreamであった, フォローユーザのアクティビティを取得する`include_friends_activity=true`はもう使えません。
- フレンドID  
  - デフォルトで有効ですが, `enable_friends`で切り替えできます。  
  - 従来のUserStreamであった, 接続開始直後に流れてきていた `{"friends": [11111, 22222, ...]}` です。  
  - 従来どおり `stringify_friend_ids=true`パラメータで文字列配列でIDを受け取れます。  
- FilterStream  
  - デフォルトでは無効です。有効にするには `filter_stream_tracks`でトラックするワードを, `filter_stream_follows`でトラックするユーザIDを設定する必要があります。  
  - 指定したワードを含むツイートや, 指定したユーザからのツイート, およびそれらの削除通知が配信されます。  
- SampleStream
  - デフォルトでは無効です。有効にするには `enable_sample_stream`で切り替える必要があります。
  - Twitterに投稿されるツイートの一部とそれらの削除通知が配信されます。

これら以外にも従来のUserStreamで配信されていたデータもありますが, APIの仕様変更によりTweetstormはそれらを提供できません。

## Wiki
セットアップ, 互換性などのセクションは [Wiki](https://github.com/SlashNephy/Tweetstorm/wiki) に移動しました。  
