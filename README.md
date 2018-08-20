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
  
## 設定
`config.json` に記載します。  
```json
{
    "host": "192.168.2.2",  // サーバを起動するホスト
    "port": 7650,  // サーバを起動するポート番号
    "accounts": [
        {
            "sn": "SlashNephy",  // アカウントの表示名, 必ずしもscreen nameと一致する必要はありません
            "id": 1000000000000000,  // アカウントの数値ID
            "ck": "sssss",  // 使用するクライアントのConsumer Key
            "cs": "ttttt",  // Consumer Secret
            "at": "xxxxx-yyyyy",  // Access Token
            "ats": "zzzzz",  // Access Token Secret
            "list_id": 200000000000  // タイムラインとみなすリストのID
        }
    ]
}
```
`ck`, `at`などの資格情報は DNSオーバライド等でサードパーティクライアント内で使用する際, それらの`ck`, `cs`に対応する`at`, `ats`が必要です。

## 使い方
```
git clone https://github.com/SlashNephy/Tweetstorm
cd Tweetstorm

vi config.json

./gradle run
```

サードパーティクライアントでエンドポイントを上書きして使用する際には 自己署名証明書等が必要です。  
iOSでは feather で動作を確認しました。[画面収録したやつ](https://www.youtube.com/watch?v=XJoFay0Og1w)



## ライセンス
このプロジェクトは MITライセンスで提供されます。
