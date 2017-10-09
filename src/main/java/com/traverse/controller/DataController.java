package com.traverse.controller;


import com.amazonaws.services.s3.model.S3Object;
import com.traverse.data.Audio;
import com.traverse.data.User;
import com.traverse.data.cloud.AudioDatabase;
import com.traverse.data.cloud.UserDatabase;
import com.traverse.exceptions.UserDoesNotExistException;
import net.bramp.ffmpeg.FFmpeg;
import net.bramp.ffmpeg.FFmpegExecutor;
import net.bramp.ffmpeg.FFprobe;
import net.bramp.ffmpeg.builder.FFmpegBuilder;
import org.apache.commons.io.IOUtils;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import java.io.IOException;
import java.util.stream.Collectors;


@RestController
@RequestMapping(value = {"/data"})
public class DataController {

    @Autowired
    private UserDatabase userDatabase;

    @Autowired
    private AudioDatabase audioDatabase;

    @RequestMapping(value = "/user/{id}", method = RequestMethod.GET)
    public String getUser(@PathVariable("id") String id, HttpServletResponse httpServletResponse) throws UserDoesNotExistException, IOException {
        String response = userDatabase.getUserJson(id);
        if (response == null){
            httpServletResponse.sendError(HttpStatus.NOT_FOUND.value(), "User does not exist.");
        }
        return response;
    }

    @RequestMapping(value = "/user/{id}/setUsername", method = RequestMethod.GET)
    public void createUser(@RequestParam("username") String username, @PathVariable("id") String id, HttpServletRequest httpServletRequest, HttpServletResponse httpServletResponse) throws IOException {
        if (userDatabase.doesUserExist(username)){
            httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "User " + username + " already exists.");
            return;
        }
        User user = userDatabase.getUserByID(id);
        if (user == null){
            httpServletResponse.sendError(HttpServletResponse.SC_NOT_FOUND, "No user with id: " + id);
            return;
        }
        userDatabase.update(user);
        httpServletResponse.sendError(HttpServletResponse.SC_OK, "Success");
    }

    @RequestMapping(value = "/audio/{id}", method = RequestMethod.GET)
    public String getAudio(@PathVariable("id") String id, HttpServletResponse httpServletResponse) throws IOException {
        String response = audioDatabase.getAudioJson(id);
        if (response == null){
            httpServletResponse.sendError(HttpStatus.NOT_FOUND.value(), "Audio does not exist.");
        }
        return response;
    }

    @RequestMapping(value = "/audio/getFile", method = RequestMethod.GET)
    public void getAudioFile(@RequestParam("audioID") String audioID, HttpServletResponse response) throws IOException {

        Audio audio = audioDatabase.getAudio(audioID);

        if (audio == null){
            response.sendError(HttpServletResponse.SC_NOT_FOUND);
            return;
        }

        S3Object object = audioDatabase.getAudioS3Object(audioID);
        response.setHeader("Content-disposition", "attachment; filename=" + audio.getName() + "." + object.getObjectMetadata().getUserMetadata().get("type"));
        IOUtils.copy(object.getObjectContent(), response.getOutputStream());
        response.flushBuffer();
    }

    @RequestMapping(value = "/audio/create", method = RequestMethod.POST)
    public String createAudio(
            @RequestParam(value = "file") MultipartFile file,
            @RequestParam(value = "name") String name,
            @RequestParam(value = "description") String description,
            @RequestParam(value = "ownerID") String ownerID,
            HttpServletRequest httpServletRequest,
            HttpServletResponse httpServletResponse) throws IOException {

        if (file.getSize() > 10 * 1000000){
            httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "File too big");
            return null;
        }

        Audio audio = new Audio.Builder()
                .withName(name)
                .withDescription(description)
                .withOwnerID(ownerID).build();

        User user = userDatabase.getUserByID(ownerID); //Lets check if the user exists.

        if (user == null){
            httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "User does not exist.");
            return null;
        }

        if (!audioDatabase.upload(file, audio)){
            httpServletResponse.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid file. Only mp3 and wav accepted.");
            return null;
        }

        //Audio checks here.

        user.addAudio(audio);
        audioDatabase.create(audio);    //Create audio
        userDatabase.update(user);      //Update user


        return audio.toJson();
    }




    /**
     *
     * @param startingMillis time in milliseconds search audio by upload time.
     * @param httpServletResponse
     * @return json object result.
     * @throws IOException
     */
    @RequestMapping(value = "/audio/list", method = RequestMethod.GET)
    public String getAudioList(@RequestParam(value = "id", required = false) Long startingMillis, HttpServletResponse httpServletResponse) throws IOException {
        return audioDatabase.list(50, startingMillis != null ? startingMillis : -1);
    }

}