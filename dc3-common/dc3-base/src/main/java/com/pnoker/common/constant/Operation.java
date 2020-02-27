package com.pnoker.common.constant;

/**
 * @author pnoker
 */
public interface Operation {

    interface Profile {
        String ADD = "add_profile";
        String DELETE = "delete_profile";
    }

    interface Device {
        String ADD = "add_device";
        String DELETE = "delete_device";
        String UPDATE = "update_device";
    }

    interface Point {
        String ADD = "add_point";
        String DELETE = "delete_point";
        String UPDATE = "update_point";
    }

    interface DriverInfo {
        String ADD = "add_driver_info";
        String DELETE = "delete_driver_info";
        String UPDATE = "update_driver_info";
    }

    interface PointInfo {
        String ADD = "add_point_info";
        String DELETE = "delete_point_info";
        String UPDATE = "update_point_info";
    }

}
