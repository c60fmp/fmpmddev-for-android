# FMPMDDev for Android

## 初めに
このプログラムは、FM 音源ドライバの曲データを Android デバイス(スマートフォン等)で聞くためのプレイヤです。
「PMD98」と「MXDRV」の曲データに対応しており、下記のドライバを使用しております。

* PMDWin
http://c60.la.coocan.jp/

* Portable mdx decoder
https://github.com/yosshin4004/portable_mdx


## インストール方法
ソースコードからビルドする、あるいはバイナリを直接インストールできます。
PC からバイナリをインストールする場合、下記手順にしたがってください。
* PC に SDK Platform Tools をインストール
https://developer.android.com/tools/releases/platform-tools
* バイナリファイル「app-debug.apk」をダウンロードする
* スマートフォンの「設定」→「デバイス情報」→「ビルド番号」を連打し、開発者モードに入る
* 「設定」→「システム」→「開発者向けオプション」を選択し、「USB デバッグ」をオンにする
* スマートフォンを PC に接続する
* PC でコマンドプロンプトを開き「adb install app-debug.apk」と入力し、ENTER を押す


## 環境設定
オーバーフローメニュ(縦３点)の「Setting」より、演奏ループ数とルートディレクトリ、PCMディレクトリを設定できます。
* Loop Count : 演奏回数を設定できます。
* root : 曲データおよびPCMデータのあるルートディレクトリを設定してください(例：/primary/chipcune/)。FMPMDDevで使用するすべてのファイルは、このルートディレクトリまたは下位ディレクトリに置いてください。
* ppc, pps 等：それぞれのPCMディレクトリを設定してください。
* wav : YM-2608 のリズム音源waveファイルを置いているディレクトリを設定してください。


## 使用方法
オーバーフローメニュ(縦３点)の「Setting」より、演奏ループ数とルートディレクトリ、PCMディレクトリを設定できます。
* Loop Count : 演奏回数を設定できます。
* root : 曲データおよびPCMデータのあるルートディレクトリを設定してください(例：/sdcard/chipcune/)。FMPMDDevで使用するすべてのファイルは、ルートディレクトリ及び下位ディレクトリに存在する必要があります。
* ppc, pps, etc. ：それぞれのPCMディレクトリを設定してください。
* wav : YM-2608 のリズム音源waveファイルを置いているディレクトリを設定してください。
  
zipアーカイブも通常のディレクトリと同様に扱うことができますが、検索時間がかかるため１アーカイブ100ファイル以下を推奨します。


## バージョンアップ履歴
### 2023/06/17 Ver1.0.0-alpha.1
* 初版
### 2024/02/17 Ver2.0.0-alpha-1
* Android のサポートバージョンを5～10 から 7～14に変更(ただし13, 14は未確認)
* 演奏中の曲のタイトルを表示するようにした
* ループ回数を設定できるようにした
* zipアーカイブに正式対応
* ファイルアクセスを SAF(Storage Access Framework) に変更     
* MXDRV の曲長取得精度向上
* Version1.0.0-alpha.1の バグフィックス
### 2024/04/20 Ver2.0.0-alpha-2
* Android14に対応
 
## ライセンス
* FMPMDDev for android
 MITライセンスに準じます。
* PMDWin, Portable mdx decoder
 これらのソース、ソフトウェアは各著作者が著作権を持ちます。 ライセンスに関しては、各ドキュメントを参照してください。
* 使用ライブラリ
 Apache Commons : Apache License 2.0 に基づき使用しております。
