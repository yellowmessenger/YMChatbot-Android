# YMChat
- [Installation](#installation)
- [Usage](#usage)

## Installation
### Gradle
To integrate YMChat into your Android project using gradle, specify the following configurations in App level `build.gradle` file
```gradle
repositories {
    jcenter()
    // Add these two lines 
    maven { url "https://jitpack.io" }
    maven { url "https://oss.sonatype.org/content/repositories/snapshots/" }
}

...
...
...

dependencies {
    ...
    ...
	   implementation 'com.github.yellowmessenger:YMChatbot-Android:v1.4.0
}
```
### strings.xml (Only for version v1.4.0 & above)
Add following key in you strings.xml file, this will override default file provider used by SDK.
Overriding the file provider path will avoid conflict with other app using YM CHATBOT SDK. You can use your application id and suffix it with ".fileprovider"
Example - applicationId : "com.abc.xyz" then  application_id_for_provider = com.abc.xyz.fileprovider
```xml
    <string name="application_id_for_provider">your.application.id.fileprovider</string>
```

## Usage
### Basic
Import the YMChat library in your Activity.
```java
import com.yellowmessenger.ymchat.YMChat;
import com.yellowmessenger.ymchat.YMConfig;
```

After the library is imported the basic bot can be presented with few lines as below 

Example `onCreate` method of the Activity:
```java

@Override
protected void onCreate(Bundle savedInstanceState) {
	super.onCreate(savedInstanceState);
    // Dummy bot id. (Purrs a lot)
    String botID = "x1587041004122";
	//Get YMChat instance
	YMChat ymChat = YMChat.getInstance();
	ymChat.config = new YMConfig(botId);
	setContentView(R.layout.activity_main);
	FloatingActionButton fab = findViewById(R.id.fab);
	fab.setOnClickListener(view -> {
		//Starting the bot activity
		try {
            ymChat.startChatbot(this);
          } catch (Exception e) {
           //Catch and handle the exception
            e.printStackTrace();
          }
	});
}

```

### YMConfig
YMConfig configures chatbot before it presented on the screen. It is recommended to set appropriate config before presenting the bot

#### Initialize
YMConfig requires a botID to initialize. All other settings can be changed after config has been initialised
```java
ymChat.config = new YMConfig("<BOT-ID>");
```

#### Speech recognition
Speech to text can be enabled by setting the enableSpeech flag present in config. Default value is `false`
```java
ymChat.config.enableSpeech = true
```

#### Payload
Additional payload can be added in the form of key value pair, which is then passed to the bot. The value of payload can be either Primitive type or json convertible value

```java
HashMap<String, Object> payloadData = new HashMap<>();
//Setting Payload Data
payloadData.put("some-key","some-value");
ymChat.config.payload = payloadData;
```

#### History
Chat history can be enabled by setting the `enableHistory` flag present in YMConfig and by passing `UserId` in payloadData mentioned above.
Default value for  `enableHistory` is `false`
```java
ymChat.config.enableHistory = true
```
One `UserId` should belong to only one user. Same user id need to be passed everytime SDK is intitalised.
```java
payloadData.put("UserId","user_id_of_customer");
```

### Starting the bot
Chat bot can be presented by calling `startChatbot()` and passing your Activity context as an argument
```java
ymChat.startChatbot(this);
```


### Close bot
Bot can be programatically closed using `closeBot()` function
```java
ymChat.closeBot();
```

### Bot close event
Bot close event is separetly sent and it can be handled by listening to onBotClose event as mentioned below.

```java
ymChat.onBotClose(() -> {
  Log.d("Example App", "Bot Was closed");
 });
```

### Events from bot
Events from bot can be handled using event Listeners.  Simply define the `onSuccess` method of `onEventFromBot` listener.

```java
ymChat.onEventFromBot(botEvent -> {
	switch (botEvent.getCode()){
	case "event-name": break;
	}
});
```
## Custom URL configuration (for on premise deployments)
Base url for the bot can be customized by setting `config.customBaseUrl` parameter. Use the same url used for on-prem deployment.

```java
ymChat.config.customBaseUrl = "<CUSTOM-BASE-URL>";
```
<!--
## Logging
Logging can be enabled to understand the code flow and to fix bugs. It can be enabled from config
```
YMChat.shared.enableLogging = true
```
-->


## Push Notifications
YMChat supports firebase notifications. Push notifications needs `authentication token` and `device token`


### Authentication Token
Authentication token can be set using `ymAuthenticationToken` variable. Auth token can be a unique identifier like email or UUID
```java
ymChat.config.ymAuthenticationToken = "your-token"
```

### Device Token
Device token can be set using `deviceToken` variable. Pass `fcmToken` as a parameter to this method.
```java
ymChat.config.deviceToken = "your-firebase-device-token"
```

## Dependencies
Following dependencies are used in chat bot SDK
```java
    implementation 'androidx.appcompat:appcompat:1.3.0'
    implementation 'androidx.legacy:legacy-support-v4:1.0.0'
    implementation 'com.google.android.material:material:1.3.0'
    implementation 'com.squareup.okhttp3:okhttp:4.7.2'
    implementation 'com.google.code.gson:gson:2.8.6'
    implementation 'androidx.multidex:multidex:2.0.1'

    testImplementation 'junit:junit:4.13.2'
    androidTestImplementation 'androidx.test.ext:junit:1.1.2'
    androidTestImplementation 'androidx.test.espresso:espresso-core:3.3.0'
```

## Permissions
We are declaring and asking for following permission in our manifest file
```java
    <uses-permission android:name="android.permission.INTERNET" />
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
    <uses-permission android:name="android.permission.RECORD_AUDIO" />

```
All permissions will be asked at run time except INTERNET.
For attachment (picking  file/images from phone storage)
```java
    <uses-permission android:name="android.permission.READ_EXTERNAL_STORAGE" />
```
For voice input
```java
    <uses-permission android:name="android.permission.RECORD_AUDIO" />
```

## Important
If facing problem in release build, add the following configuration in the app's proguard-rules.pro file.
```java
-keep public class com.yellowmessenger.ymchat.** {
   *;
}
```

## Demo App
A demo has been created to better understand the integration of SDK in Android app
[https://github.com/yellowmessenger/YmChatBot-Android-DemoApp](https://github.com/yellowmessenger/YmChatBot-Android-DemoApp)


