# Traverse
Online open source music sharing service

https://traverse.dax.cloud/


#### Running server on localhost:8080

###### Requires 
* Apache Maven 3.3.9
* Docker 1.12.6
* JDK 1.8

###### Notes: 
* Make sure your java compiler is set to 1.8
    * update-alternatives --config javac

Create your applications.properties file in src\main\resources         

Fill in cloud.aws_access_key_id and cloud.aws_secret_access_key with your AWS IAM key. 
Make sure your IAM key has Administrator Access.
We will be creating and modifying dynamodb and tables which will require those access permissions.


#### Facebook Login
- Fill in spring.social.facebook.appId and spring.social.facebook.appSecret with your Facebook developer app key.
You can create an app over on developers.facebook.com. Make sure you set your callback url to the correct address
on Facebook.
- Also make sure to replace the facebook app id to yours in the FB.init method in index.html for oauth login.

Max file size accepted for upload is 20MB, although you can adjust this value in application.properties.

```
spring.mvc.view.suffix=.html

server.port=8080

cloud.dynamoDB_table_name_users=traverse_users
cloud.dynamoDB_table_name_audio=traverse_audio
cloud.dynamoDB_table_name_auth=traverse_auth
cloud.s3_bucket=traversebucket

spring.http.multipart.max-file-size=20MB

cloud.aws_access_key_id=
cloud.aws_secret_access_key=

spring.social.facebook.appId=
spring.social.facebook.appSecret=
```

If you have existing dynamoDB tables that conflict with the table names in your AWS console, please delete them. We have updated our database schema since last build.
```
cloud.dynamoDB_table_name_users=traverse_users
cloud.dynamoDB_table_name_audio=traverse_audio
cloud.dynamoDB_table_name_auth=traverse_auth
```

After that is finished, you can begin to build and deploy.

If this is the first time you are deploying, Spring Boot initialization will take a couple minutes.
This is because upon first initialization, the server will need to build the dynamoDB tables and S3 Buckets and wait for them to be active on Amazon servers.
Any calls made before then will **not work**.


```
mvn install
docker build -f Dockerfile -t traverse .
docker run -p 8080:8080 traverse
```


## Testing backend functionality
View the rest of the endpoints on our wiki.

### Login
- First, use facebook button to login. After Logging in, you would need to refresh the page to make sure **"we are connected"** is up. Once that message is up, you have successfully logged in.

![](https://i.imgur.com/bMcwR6F.png)

### Getting your user ID
- A GET call to this url will now provide your user ID after you login with facebook. 
Make sure you are in the same browser because we will be using your cookies to determine this information.
    - GET /oauth/getUser
    
![](https://i.imgur.com/9R9X30L.png)


### Grabbing your user info using user  ID
- You can grab the user's information using this ID.
    - GET /data/user/{userID}
    
![](https://i.imgur.com/5o85RGa.png)


### Uploading Audio
- Since we don't have a front upload audio button, you will need a HTTP testing client to test the POST call. 
I recommend postman so you are able to follow the screenshot below. 
    - POST /data/audio/create
        - name: song name
        - ownerID: your userID
        - description: description of the song
        - file: your song file.
    - Returns: The Audio data in json format.

![](https://i.imgur.com/Vg01D76.png)


### Grabbing Audio
- You can now check if the song is uploaded by grabbing the audio.
    - GET data/audio/getFile?audioID={audioID}
        - audioID: The ID of the audio.

![](https://i.imgur.com/avt0PNb.png)

TRIVIAL JCNORRIS EDIT
