package com.example.raft.client;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;


@RestController
@RequestMapping("/raft")
public class TestController {
    @Autowired
    private RaftTemplate raftTemplate;

    @PostMapping("/write")
    public String write(@RequestParam String key, @RequestParam String value) {
        return raftTemplate.write(key, value);
    }

    @GetMapping("/read")
    public String read(@RequestParam String key) {
        return raftTemplate.read(key);
    }
}
