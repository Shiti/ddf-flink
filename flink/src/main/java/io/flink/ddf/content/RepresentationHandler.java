/*
 * Copyright 2014, Tuplejump Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package io.flink.ddf.content;

import io.ddf.DDF;

/**
 * User: satya
 */
public class RepresentationHandler extends io.ddf.content.RepresentationHandler {

    public RepresentationHandler(DDF theDDF) {
        super(theDDF);
        this.addConvertFunction(Conversions.stringDataSet, Conversions.objectArrDataSet, new Conversions.StringDataSetToObjectArrDataSet(theDDF));
        this.addConvertFunction(Conversions.objectArrDataSet, Conversions.mr_data, new Conversions.ObjectArrDataSetToMRFlink(theDDF));
        this.addConvertFunction(Conversions.mr_data, Conversions.objectArrDataSet, new Conversions.MRDataToObjectArr(theDDF));
    }


}
