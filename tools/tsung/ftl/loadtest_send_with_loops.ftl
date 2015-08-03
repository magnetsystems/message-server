<?xml version="1.0"?>
<!DOCTYPE tsung SYSTEM "/usr/local/Cellar/tsung/1.5.0/share/tsung/tsung-1.0.dtd" [] >
<tsung loglevel="notice" version="1.0" dumptraffic="true">


    <clients>
        <client host="localhost" use_controller_vm="true"></client>
        <!--client host="localhost" maxusers="100"></client-->
    </clients>

    <servers>
        <server host="${mmxHostname}" port="${mmxPort}" type="tcp"></server>
    </servers>

    <load>
        <arrivalphase phase="1" duration="5" unit="minute">
            <users arrivalrate="5" unit="second"></users>
        </arrivalphase>
    </load>


    <options>
        <option name="file_server" id="userdb" value="userdb.csv"/>
    </options>

    <sessions>
        <session probability="100" name="jabber-example" type="ts_jabber">
            <setdynvars sourcetype="file" fileid="userdb" delimiter=";" order="iter">
                <var name="username"/>
                <var name="password"/>
                <var name="tojid"/>
            </setdynvars>

            <request subst='true'>
                <jabber type="connect" ack="local">
                    <xmpp_authenticate username="%%_username%%" passwd="%%_password%%"/>
                </jabber>
            </request>

            <request>
                <jabber type="connect" ack="local"></jabber>
            </request>

            <thinktime value="2"></thinktime>

            <transaction name="authenticate">
                <request>
                    <jabber type="auth_get" ack="local"></jabber>
                </request>
                <request>
                    <jabber type="auth_set_plain" ack="local"></jabber>
                </request>
            </transaction>

            <request>
                <jabber type="presence:initial" ack="local"></jabber>
            </request>
            <!-- send stanzas in a loop -->
            <for from="1" to="1" incr="1" var="counter">
            <request subst="true">
                <jabber type="raw" ack="local" data="${mmx_stanza}"></jabber>
            </request>
            <thinktime value="3"></thinktime>
            </for>

            <transaction name="close">
                <request>
                    <jabber type="close" ack="local"></jabber>
                </request>
            </transaction>
        </session>
    </sessions>
</tsung>
