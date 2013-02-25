shd.open.jira-hipchat-hook
==========================

converts jira webhook requests to hipchat API calls using velocity to render the message.

usage
-----

* update the jira.properties sample file, esp. add your hipchat API token and the id of the room you want the message
to be sent to.
* deploy the app to your servlet container
* add a new webhook to your jira instance; eg. when deployed under context 'jira-hipchat-hook':
`http://yourserver:port/jira-hipchat-hook/convert?config=jira`
* whenever jira calls the webhook the jira.properties config will be used to render a message and send it to hipchat
* multiple configs can be triggered by adding them to the webhook url: eg.
`http://yourserver:port/jira-hipchat-hook/convert?config=jira&config=another`

why?
----

because the current hipchat plugin for jira requires modifications to the workflow; and the room is configured there,
so multiple projects need separate workflows if you want to message different rooms. total no-go.

disclaimer
----------

this is an _initial quick-and-dirty proof of concept_. use at your own risk.
