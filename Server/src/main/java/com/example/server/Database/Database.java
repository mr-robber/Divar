package com.example.server.Database;

import com.example.server.Database.Messages.Messages;
import com.example.server.Database.Posts.Post;
import com.example.server.Database.Users.Users;
import com.mongodb.client.*;
import org.bson.Document;
import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;


public class Database {
    private static MongoClient mongoClient;
    private static MongoDatabase database;
    private static MongoCollection<Document> collection;
    public static int imageID;
    public static int profileImageID;

    public static int lastImageID(){
        imageID ++;
        return imageID;
    }

    public static int lastProfileImageID(){
        profileImageID ++;
        return profileImageID;
    }

    public static void connectToDatabase() {
        mongoClient = MongoClients.create();
        database = mongoClient.getDatabase("Divar");
    }

    public synchronized static void addPost(Post post) {
        connectToDatabase();
        collection = database.getCollection("Posts");
        collection.insertOne(post.getDocument());
        disconnect();
        Users users = new Users(post.getDocument().getString("phoneNumber"));
        updateUserArrays(users, "userPosts");
    }

    public synchronized static void addUser(Users users) {
        connectToDatabase();
        collection = database.getCollection("Users");
        if (!collection.find(users.getDocument()).cursor().hasNext()) {
            collection.insertOne(users.getDocument());
        }
        disconnect();
    }

    public synchronized static void updatePost(Post post ,String key ,Object value) {
        connectToDatabase();
        collection = database.getCollection("Posts");
        collection.updateOne(post.getFilterDocument() ,new Document("$set" ,new Document(key ,value)));
        disconnect();
    }

    public synchronized static void updateUser(Users users ,String key ,Object value) {
        connectToDatabase();
        collection = database.getCollection("Users");
        collection.updateOne(users.getDocument() ,new Document("$set" ,new Document(key ,value)));
        disconnect();
    }

    public synchronized static Document findPost(Document filter) {
        connectToDatabase();
        Document document = new Document("", "");
        collection = database.getCollection("Posts");
        if (collection.find(filter).cursor().hasNext()) {
            document = collection.find(filter).cursor().next();
        }
        disconnect();
        return document;
    }

    public static String getPost(Document filter) {
        return findPost(filter).toJson();
    }

    public synchronized static Document findUser(Document filter) {
        connectToDatabase();
        Document document = new Document("", "");
        collection = database.getCollection("Users");
        if (collection.find(filter).cursor().hasNext()) {
            document = collection.find(filter).cursor().next();
        }
        disconnect();
        return document;
    }

    public static String getUser(Document filter) {
        return findUser(filter).toJson();
    }

    public synchronized static void deletePost(Post post) {
        connectToDatabase();
        collection = database.getCollection("Posts");
        if (collection.find(post.getFilterDocument()).cursor().hasNext()) {
            collection.deleteOne(post.getFilterDocument());
        }
        disconnect();
    }

    public synchronized static void deleteUser(Users users) {
        connectToDatabase();
        collection = database.getCollection("Users");
        if (collection.find(users.getDocument()).cursor().hasNext()) {
            collection.deleteOne(users.getDocument());
        }
        disconnect();
    }

    public static boolean isPostExits(Post post){
        connectToDatabase();
        collection = database.getCollection("Posts");
        boolean flag = collection.find(post.getFilterDocument()).cursor().hasNext();
        disconnect();
        return flag;
    }

    public static boolean isUserExits(Users users){
        connectToDatabase();
        collection = database.getCollection("Users");
        boolean flag = collection.find(users.getDocument()).cursor().hasNext();
        disconnect();
        return flag;
    }

    public synchronized static void updateUserArrays(Users users, String arrayName) {
        JSONObject user = new JSONObject(getUser(users.getDocument()));
        ArrayList<String> Posts;
        if (user.has(arrayName)) {
            Posts = getStringArray(user.getJSONArray(arrayName));
            Posts.add(String.valueOf(lastPostId()));
        } else {
            Posts = new ArrayList<>();
            Posts.add(String.valueOf(lastPostId()));
        }
        updateUser(users, arrayName, Posts);
    }

    public static int lastPostId() {
        connectToDatabase();
        collection = database.getCollection("Posts");
        int lastId = 0;
        if (collection.find().sort(new Document("postId", -1)).limit(1).cursor().hasNext()) {
            String jsonString = collection.find().sort(new Document("postId", -1)).limit(1).cursor().next().toJson();
            JSONObject obj = new JSONObject(jsonString);
            lastId = obj.getInt("postId");
        }
        disconnect();
        return lastId;
    }

    public static int lastImageIdOfPosts() {
        int lastPostId = lastPostId();
        connectToDatabase();
        collection = database.getCollection("Posts");
        int lastId = 1;
        for (int i = lastPostId; i >= 0; i--) {
            if (collection.find(new Document("postId", i)).cursor().hasNext()) {
                String jsonString = collection.find(new Document("postId", i)).cursor().next().toJson();
                JSONObject post = new JSONObject(jsonString);
                JSONArray imageName = post.getJSONArray("imageName");
                int temp = imageName.getInt(imageName.length() - 1);
                if (temp > lastId) {
                    lastId = temp;
                }
            }
        }
        disconnect();
        return lastId;
    }

    public static ArrayList<String> getNotAcceptedPosts() {
        connectToDatabase();
        collection = database.getCollection("Posts");
        ArrayList<String> notAccepted = new ArrayList<>();
        if(collection.find(new Document("accept", false)).cursor().hasNext()) {
            for (Document document : collection.find(new Document("accept", false))) {
                notAccepted.add(document.toJson());
            }
        }
        disconnect();
        return notAccepted;
    }

    public static ArrayList<String> getPosts(int number, String key, String value) {
        connectToDatabase();
        collection = database.getCollection("Posts");
        int temp = number;
        ArrayList<String> posts = new ArrayList<>();
        if (collection.find(new Document(key, value)).cursor().hasNext()) {
            for (Document document : collection.find(new Document(key, value))) {
                if (temp == 0) {
                    break;
                }
                posts.add(document.toJson());
                temp--;
            }
        }
        disconnect();
        return posts;
    }

    public static ArrayList<String> lastSeenPost(Document filter) {
        String user = getUser(filter);
        JSONObject jsonObject = new JSONObject(user);
        JSONArray jsonArray = jsonObject.getJSONArray("lastSeenPost");
        ArrayList<String> lastSeen = new ArrayList<>();
        for (int i = 0; i < jsonArray.length(); i++) {
            lastSeen.add(getPost(new Document("postId", jsonArray.get(i))));
        }
        return lastSeen;
    }

    public static Users getUserAsDoc(Document filter) {
        return new Users(filter).setValues(findUser(filter));
    }



    public static void disconnect(){
        mongoClient.close();
    }

    public static ArrayList<String> getMarkedPosts(int size, Users user){
        String temp = getUser(user.getDocument());
        JSONObject object = new JSONObject(temp);
        JSONArray jsonArray = object.getJSONArray("bookmarkPost");
        ArrayList<String> markedPost = new ArrayList<>();
        for (int i = user.getNumberForMarkedPost() * size; i < (size * user.getNumberForMarkedPost()) + size; i++) {
            if(i < jsonArray.length()) {
                markedPost.add(getPost(new Document("postId", jsonArray.get(i))));
            }
        }
        user.setNumberForMarkedPost(user.getNumberForMarkedPost() + 1);
        return markedPost;
    }


    public static ArrayList<String> getUsersPosts(int size, Users user){
        String temp = getUser(user.getDocument());
        JSONObject object = new JSONObject(temp);
        ArrayList<String> jsonArray = getStringArray(object.getJSONArray("userPosts"));
        ArrayList<String> usersPost = new ArrayList<>();
        for (int i = user.getNumberForUsersPost() * size; i < (size * user.getNumberForUsersPost()) + size; i++) {
            if(i < jsonArray.size()) {
                usersPost.add(getPost(new Document("postId", Integer.parseInt(jsonArray.get(i)))));
            }
        }
        user.setNumberForUsersPost(user.getNumberForUsersPost() + 1);
        return usersPost;
    }

    public static void addMessage(Messages messages){
        connectToDatabase();
        collection = database.getCollection("Messages");
        collection.insertOne(messages.getDocument());
        disconnect();
    }

    public static void updateMessage(Messages messages ,String key ,Object value){
        connectToDatabase();
        collection = database.getCollection("Messages");
        collection.updateOne(messages.getFilterDocument() ,new Document("$set" ,new Document(key ,value)));
        disconnect();
    }

    public synchronized static void deleteMessage(Messages messages) {
        connectToDatabase();
        collection = database.getCollection("Messages");
        if (collection.find(messages.getFilterDocument()).cursor().hasNext()) {
            collection.deleteOne(messages.getFilterDocument());
        }
        disconnect();
    }

    public synchronized static Document findMessage(Document filter) {
        connectToDatabase();
        Document document = new Document("", "");
        collection = database.getCollection("Messages");
        if (collection.find(filter).cursor().hasNext()) {
            document = collection.find(filter).cursor().next();
        }
        disconnect();
        return document;
    }

    public synchronized static ArrayList<String> findMessageCount(Document filter) {
        connectToDatabase();
        Document document = new Document("", "");
        ArrayList<String> temp = new ArrayList<>();
        collection = database.getCollection("Messages");
        if (collection.find(filter).cursor().hasNext()) {
            for (Document doc: collection.find(filter)) {
                temp.add(doc.getString("user2"));
            }
        }
        disconnect();
        return temp;
    }

    public static String getMessage(Document filter) {
        return findMessage(filter).toJson();
    }

    public static int numberOfPostsOfUser(Users users) {
        connectToDatabase();
        int number = 0;
        String user = getUser(users.getDocument());
        JSONObject object = new JSONObject(user);
        JSONArray jsonArray = object.getJSONArray("userPosts");
        number = jsonArray.length();
        disconnect();
        return number;
    }

    public static int lastUserImageId() {
        connectToDatabase();
        collection = database.getCollection("Users");
        int lastId = 1;
        if (collection.find().cursor().hasNext()) {
            for (Document document : collection.find()) {
                JSONObject doc = new JSONObject(document.toJson());
                if (doc.has("profileNameImage")) {
                    int temp = document.getInteger("profileNameImage");
                    if (temp > lastId) {
                        lastId = temp;
                    }
                }

            }
        }
        disconnect();
        return lastId;
    }

    public static ArrayList<String> getStringArray (JSONArray JArray) {
        ArrayList<String> list = new ArrayList<>();
        for (int i = 0; i < JArray.length(); i++) {
            list.add(JArray.getString(i));
        }
        return list;
    }

    public static ArrayList<String> search(int size, String search) {
        connectToDatabase();
        int temp = size;
        collection = database.getCollection("Posts");
        ArrayList<String> posts = new ArrayList<>();
        if (collection.find().cursor().hasNext()) {
            for (Document document : collection.find()) {
                if (temp == 0) {
                    break;
                }
                String title = document.getString("title");
                if (title.contains(search)) {
                    posts.add(document.toJson());
                    temp--;
                }
            }
        }
        disconnect();
        return posts;
    }

}