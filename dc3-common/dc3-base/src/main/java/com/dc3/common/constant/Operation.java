/*
 * Copyright 2018-2020 Pnoker. All Rights Reserved.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.dc3.common.constant;

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
