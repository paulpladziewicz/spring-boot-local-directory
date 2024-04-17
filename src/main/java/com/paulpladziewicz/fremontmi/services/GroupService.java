package com.paulpladziewicz.fremontmi.services;

import com.paulpladziewicz.fremontmi.models.Group;
import org.springframework.stereotype.Service;

@Service
public class GroupService {

    public void createGroup(Group group) {
        System.out.println("createGroup called");
    }
}
