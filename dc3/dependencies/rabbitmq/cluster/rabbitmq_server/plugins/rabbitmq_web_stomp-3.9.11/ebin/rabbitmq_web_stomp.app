{application, 'rabbitmq_web_stomp', [
	{description, "RabbitMQ STOMP-over-WebSockets support"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['rabbit_web_stomp_app','rabbit_web_stomp_connection_sup','rabbit_web_stomp_handler','rabbit_web_stomp_internal_event_handler','rabbit_web_stomp_listener','rabbit_web_stomp_middleware','rabbit_web_stomp_stream_handler','rabbit_web_stomp_sup']},
	{registered, [rabbitmq_web_stomp_sup]},
	{applications, [kernel,stdlib,cowboy,rabbit_common,rabbit,rabbitmq_stomp]},
	{mod, {rabbit_web_stomp_app, []}},
	{env, [
	    {tcp_config, [{port, 15674}]},
	    {ssl_config, []},
	    {num_tcp_acceptors, 10},
	    {num_ssl_acceptors, 10},
	    {cowboy_opts, []},
	    {proxy_protocol, false},
	    {ws_frame, text},
	    {use_http_auth, false}
	  ]},
		{broker_version_requirements, []}
]}.