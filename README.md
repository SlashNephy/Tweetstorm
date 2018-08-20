English README is [here](https://github.com/SlashNephy/Tweetstorm/blob/master/README.md).  

# Tweetstorm
Twitter UserStream APIの簡単な代替実装  

## 概要
Tweetstormの目的は2018/8/23に完全停止する(した)のUserStream APIを再現することです。  
Tweetstormは以下のJSONデータを提供します。  
- ツイート  
  - homeタイムラインのレートリミットは15回/15分であるのに対し, listタイムラインは900回/15分なので毎秒更新できます。
  - リストを毎秒更新することで実質的なリアルタイムを実現しています。
- [未実装] ダイレクトメッセージ  
- [未実装] イベント/アクティビティ  
  - `favorite`, `unfavorite`, `follow`, ...
  - AAA (Account Activity API)が利用できなくても提供するように実装する予定です。
- フレンド  
  - UserStream接続開始直後に流れてきていた `{"friends": [11111, 22222, ...]}` です。
  - `stringify_friend_ids=true`パラメータで従来どおり文字列の配列でIDを受け取れます。
- [未実装] 削除  
  - UserStream接続中に削除されたツイートのIDが流れてきていた `{"delete": {"status_id": 10000, ...}}` です。
  - AAAが利用可能な場合に限ります。

一方で次のデータは**提供されません**。
- ツイート検閲
  - UserStreamで取得可能でしたがAAAでも提供されていません。
- リミット
  - 同上。もはやリミットはありません。
- 一部のイベント
  - 調査中です。
  
## セットアップ
[こっち](https://github.com/SlashNephy/Tweetstorm/wiki/%E3%82%BB%E3%83%83%E3%83%88%E3%82%A2%E3%83%83%E3%83%97)に移動しました。  
iOSでは feather で動作を確認しました。[画面収録したやつ](https://www.youtube.com/watch?v=XJoFay0Og1w)  

## ライセンス
このプロジェクトは MITライセンスで提供されます。
