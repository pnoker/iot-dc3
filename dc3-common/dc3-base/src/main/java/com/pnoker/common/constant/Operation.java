package com.pnoker.common.constant;

/**
 * @author pnoker
 */
public interface Operation {

    interface Device {
        String ADD = "add_device";
        String DELETE = "delete_device";
        String UPDATE = "update_device";
    }

    interface Profile {
        String ADD = "add_profile";
        String DELETE = "delete_profile";
        String UPDATE = "update_profile";
    }
}
