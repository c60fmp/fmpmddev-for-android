# FMPMDDev for android

## 初めに
このプログラムは、FM 音源ドライバの曲データを Windows 上で聞くためのプレイヤです。
「PMD98」と「MXDRV」の曲データに対応しており、下記のドライバを使用しております。

* PMDWin
http://c60.la.coocan.jp/

* Portable mdx decoder
https://github.com/yosshin4004/portable_mdx


## 環境設定
オーバーフローメニュ(縦３点)の「Set Directory」より、ルートディレクトリ、PCMディレクトリを設定できます。
* root : 曲データのあるルートディレクトリを設定して下さい(例：/sdcard/chipcune/)
* wav : YM-2608 のリズム音源waveファイルを置いているディレクトリを設定して下さい。


## 既知のバグ
* ナビゲーションバーの「戻る」ボタンを押したとき、あるいはリストの「..」をタップしたときに、意図したディレクトリに遷移しないことがある。
* MXDRVの曲データにおいて正常に発音しないことがある(再起動で復帰するケースと復帰しないケースがある)


## ライセンス
* FMPMDDev for android
 MITライセンスに準じます。
* PMDWin, Portable mdx decoder
 これらのソース、ソフトウェアは各著作者が著作権を持ちます。 ライセンスに関しては、各ドキュメントを参照してください。
* 使用ライブラリ
 Apache Commons : Apache License 2.0 に基づき使用しております。
