{application, 'rabbitmq_stomp', [
	{description, "RabbitMQ STOMP plugin"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['Elixir.RabbitMQ.CLI.Ctl.Commands.ListStompConnectionsCommand','rabbit_stomp','rabbit_stomp_client_sup','rabbit_stomp_connection_info','rabbit_stomp_frame','rabbit_stomp_internal_event_handler','rabbit_stomp_processor','rabbit_stomp_reader','rabbit_stomp_sup','rabbit_stomp_util']},
	{registered, [rabbitmq_stomp_sup]},
	{applications, [kernel,stdlib,ranch,rabbit_common,rabbit,amqp_client]},
	{mod, {rabbit_stomp, []}},
	{env, [
	    {default_user,
	     [{login, <<"guest">>},
	      {passcode, <<"guest">>}]},
	    {default_vhost, <<"/">>},
	    {default_topic_exchange, <<"amq.topic">>},
		{default_nack_requeue, true},
	    {ssl_cert_login, false},
	    {implicit_connect, false},
	    {tcp_listeners, [61613]},
	    {ssl_listeners, []},
	    {num_tcp_acceptors, 10},
	    {num_ssl_acceptors, 10},
	    {tcp_listen_options, [{backlog,   128},
	                          {nodelay,   true}]},
	    %% see rabbitmq/rabbitmq-stomp#39
	    {trailing_lf, true},
	    %% see rabbitmq/rabbitmq-stomp#57
	    {hide_server_info, false},
	    {proxy_protocol, false}
	  ]},
		{broker_version_requirements, []}
]}.