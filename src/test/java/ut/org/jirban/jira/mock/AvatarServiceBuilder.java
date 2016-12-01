/*
 * Copyright 2016 Red Hat, Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *   http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package ut.org.jirban.jira.mock;

import static org.mockito.Matchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.net.URI;

import com.atlassian.jira.avatar.Avatar;
import com.atlassian.jira.avatar.AvatarService;
import com.atlassian.jira.user.ApplicationUser;

/**
 * @author Kabir Khan
 */
public class AvatarServiceBuilder {
    private final AvatarService avatarService = mock(AvatarService.class);

    private AvatarServiceBuilder() {
    }

    public static AvatarService getUserNameUrlMock() {
        AvatarServiceBuilder builder = new AvatarServiceBuilder();
        return builder.build();
    }

    AvatarService build() {
        when(avatarService.getAvatarURL(any(ApplicationUser.class), any(ApplicationUser.class), any(Avatar.Size.class)))
                .then(invocation -> {
                    ApplicationUser user = (ApplicationUser)invocation.getArguments()[1];
                    String name = user.getName();
                    return new URI("/avatars/" + name + ".png");
                });
        return avatarService;
    }

}
