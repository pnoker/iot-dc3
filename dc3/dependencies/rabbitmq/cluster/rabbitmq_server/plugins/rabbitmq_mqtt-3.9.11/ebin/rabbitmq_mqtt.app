{application, 'rabbitmq_mqtt', [
	{description, "RabbitMQ MQTT Adapter"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['Elixir.RabbitMQ.CLI.Ctl.Commands.DecommissionMqttNodeCommand','Elixir.RabbitMQ.CLI.Ctl.Commands.ListMqttConnectionsCommand','mqtt_machine','mqtt_machine_v0','mqtt_node','rabbit_mqtt','rabbit_mqtt_collector','rabbit_mqtt_connection_info','rabbit_mqtt_connection_sup','rabbit_mqtt_frame','rabbit_mqtt_internal_event_handler','rabbit_mqtt_processor','rabbit_mqtt_reader','rabbit_mqtt_retained_msg_store','rabbit_mqtt_retained_msg_store_dets','rabbit_mqtt_retained_msg_store_ets','rabbit_mqtt_retained_msg_store_noop','rabbit_mqtt_retainer','rabbit_mqtt_retainer_sup','rabbit_mqtt_sup','rabbit_mqtt_util']},
	{registered, [rabbitmq_mqtt_sup]},
	{applications, [kernel,stdlib,ranch,rabbit_common,rabbit,amqp_client,ra]},
	{mod, {rabbit_mqtt, []}},
	{env, [
	    {default_user, <<"guest">>},
	    {default_pass, <<"guest">>},
	    {ssl_cert_login,false},
	    %% To satisfy an unfortunate expectation from popular MQTT clients.
	    {allow_anonymous, true},
	    {vhost, <<"/">>},
	    {exchange, <<"amq.topic">>},
	    {subscription_ttl, 86400000}, %% 24 hours
	    {retained_message_store, rabbit_mqtt_retained_msg_store_dets},
	    %% only used by DETS store
	    {retained_message_store_dets_sync_interval, 2000},
	    {prefetch, 10},
	    {ssl_listeners, []},
	    {tcp_listeners, [1883]},
	    {num_tcp_acceptors, 10},
	    {num_ssl_acceptors, 10},
	    {tcp_listen_options, [{backlog,   128},
	                          {nodelay,   true}]},
	    {proxy_protocol, false},
	    {sparkplug, false}
	  ]},
		{broker_version_requirements, []}
]}.