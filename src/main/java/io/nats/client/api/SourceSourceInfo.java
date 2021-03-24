// Copyright 2020 The NATS Authors
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at:
//
// http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

package io.nats.client.api;

import java.util.ArrayList;
import java.util.List;

import static io.nats.client.support.JsonUtils.getObjectList;
import static io.nats.client.support.SchemaConstants.SOURCE;
import static io.nats.client.support.SchemaConstants.SOURCES;

public class SourceSourceInfo extends SourceInfo {

    public static List<SourceSourceInfo> optionalListOf(String json) {
        List<String> strObjects = getObjectList(SOURCES, json);
        List<SourceSourceInfo> list = new ArrayList<>();
        for (String j : strObjects) {
            list.add(new SourceSourceInfo(j));
        }
        return list.isEmpty() ? null : list;
    }

    public SourceSourceInfo(String json) {
        super(json, SOURCE);
    }
}
