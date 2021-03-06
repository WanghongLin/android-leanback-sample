/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package android.support.v17.leanback.widget;

import android.annotation.TargetApi;
import android.content.res.Resources;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Build;
import android.view.View;
import android.view.ViewGroup;

import com.example.android.leanback.R;

@TargetApi(Build.VERSION_CODES.L)
class ShadowHelperApi21 {

    static int sNormalZ = Integer.MIN_VALUE;
    static int sFocusedZ;

    private static void initializeResources(Resources res) {
        if (sNormalZ == Integer.MIN_VALUE) {
            sNormalZ = (int) res.getDimension(R.dimen.lb_material_shadow_normal_z);
            sFocusedZ = (int) res.getDimension(R.dimen.lb_material_shadow_focused_z);
        }
    }

    /* add shadows and return a implementation detail object */
    public static Object addShadow(ViewGroup shadowContainer) {
        initializeResources(shadowContainer.getResources());
        shadowContainer.setBackground(new ColorDrawable(Color.TRANSPARENT));
        shadowContainer.setZ(sNormalZ);
        return shadowContainer;
    }

    /* set shadow focus level 0 for unfocused 1 for fully focused */
    public static void setShadowFocusLevel(Object impl, float level) {
        ViewGroup shadowContainer = (ViewGroup) impl;
        shadowContainer.setZ(sNormalZ + level * (sFocusedZ - sNormalZ));
    }

    public static void setZ(View view, float level) {
        initializeResources(view.getResources());
        view.setZ(sNormalZ + level * (sFocusedZ - sNormalZ));
    }
}
