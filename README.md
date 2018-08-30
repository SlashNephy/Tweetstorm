English README is [here](https://github.com/SlashNephy/Tweetstorm/blob/master/README_EN.md).  

# Tweetstorm: UserStream APIの簡単な代替実装  
Tweetstormはクライアントに代わってREST APIを呼び出し, 従来のTwitter UserStream APIと同等のインターフェイスで配信します。

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
 
Tweetstormは以下のJSONデータを提供します。  
- ツイート  
  - Homeタイムラインのレートリミットは15回/15分であるのに対し, Listタイムラインは900回/15分なので毎秒更新できます。
  - リストを毎秒更新することで実質的なリアルタイムを実現しています。
  - リストに自分が入っていない場合は, 別途にUserタイムライン, Mentionタイムラインを呼び出し, 配信します。
- フレンドID  
  - UserStream接続開始直後に流れてきていた `{"friends": [11111, 22222, ...]}` です。
  - `stringify_friend_ids=true`パラメータで従来どおり文字列の配列でIDを受け取れます。

また, 次のデータは未実装ですが, 今後提供される可能性があります。
- ダイレクトメッセージ  
- アクティビティ  
  - `favorite`, `unfavorite`, `follow`, ...
  - AAA (Account Activity API)が利用できなくても提供するように実装する予定です。
  - `include_following_activity=true`はもう使えません。
- 削除通知  
  - UserStream接続中に削除されたツイートのIDが流れてきていた `{"delete": {"status_id": 10000, ...}}` です。
  - AAAが利用可能な場合に限ります。

これら以外にも従来のUserStreamで配信されていたデータもありますが, API仕様変更によりTweetstormは提供できません。

## Wiki
セットアップ, 互換性などのセクションは [Wiki](https://github.com/SlashNephy/Tweetstorm/wiki) に移動しました。

## ライセンス
このプロジェクトは MITライセンスで提供されます。
