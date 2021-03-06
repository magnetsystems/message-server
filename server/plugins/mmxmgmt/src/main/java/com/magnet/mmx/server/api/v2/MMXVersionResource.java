/*   Copyright (c) 2015 Magnet Systems, Inc.
 *
 *  Licensed under the Apache License, Version 2.0 (the "License");
 *  you may not use this file except in compliance with the License.
 *  You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 *  Unless required by applicable law or agreed to in writing, software
 *  distributed under the License is distributed on an "AS IS" BASIS,
 *  WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 *  See the License for the specific language governing permissions and
 *  limitations under the License.
 */
package com.magnet.mmx.server.api.v2;

import javax.ws.rs.GET;
import javax.ws.rs.Path;
import javax.ws.rs.Produces;
import javax.ws.rs.core.Context;
import javax.ws.rs.core.HttpHeaders;
import javax.ws.rs.core.MediaType;
import javax.ws.rs.core.Response;

import com.magnet.mmx.server.plugin.mmxmgmt.MMXVersion;

/**
 * Get the version without authentication.
 */
@Path("mmx/version")
public class MMXVersionResource {
  @GET
  @Produces(MediaType.APPLICATION_JSON)
  public Response getVersion(@Context HttpHeaders headers) {
    return Response.status(Response.Status.OK).entity(new Version(MMXVersion.getVersion())).build();
  }

  public static class Version {
    private String version;

    public Version(String version) {
      this.version = version;
    }

    public String getVersion() {
      return version;
    }
  }
}
