# YMChat
- [Installation](#installation)
- [Usage](#usage)

# Installation
## Gradle
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
	   implementation 'com.github.yellowmessenger:YMChatbot-Android:v1.2.0
}
```
  
# Usage
## Basic
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

## YMConfig
YMConfig configures chatbot before it presented on the screen. It is recommended to set appropriate config before presenting the bot

### Initialize
YMConfig requires a botID to initialize. All other settings can be changed after config has been initialised
```java
ymChat.config = new YMConfig("<BOT-ID>");
```

### Speech recognition
Speech to text can be enabled by setting the enableSpeech flag present in config. Default value is `false`
```java
ymChat.config.enableSpeech = true
```

### Payload
Additional payload can be added in the form of key value pair, which is then passed to the bot. The value of payload can be either Primitive type or Map<String,String>
```java
HashMap<String, Object> payloadData = new HashMap<>();
//Setting Payload Data
payloadData.put("some-key","some-value");
ymChat.config.payload = payloadData;
```

### History
Chat history can be enabled by setting the `enableHistory` flag present in YMConfig. Default value is `false`
```java
ymChat.config.enableHistory = true
```

## Starting the bot
Chat bot can be presented by calling `startChatbot()` and passing your Activity context as an argument
```java
ymChat.startChatbot(this);
```


## Close bot
Bot can be programatically closed using `closeBot()` function
```java
ymChat.closeBot();
```

## Events from bot
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


## Important
If facing problem in release build, add the following configuration in the app's proguard-rules.pro file.
```java
-keep public class com.example.ymwebview.** {
   *;
}
```


