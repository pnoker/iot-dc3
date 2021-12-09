{application, 'rabbitmq_tracing', [
	{description, "RabbitMQ message logging / tracing"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['rabbit_tracing_app','rabbit_tracing_consumer','rabbit_tracing_consumer_sup','rabbit_tracing_files','rabbit_tracing_mgmt','rabbit_tracing_sup','rabbit_tracing_traces','rabbit_tracing_util','rabbit_tracing_wm_file','rabbit_tracing_wm_files','rabbit_tracing_wm_trace','rabbit_tracing_wm_traces']},
	{registered, [rabbitmq_tracing_sup]},
	{applications, [kernel,stdlib,rabbit_common,rabbit,rabbitmq_management]},
	{mod, {rabbit_tracing_app, []}},
	{env, [
	    {directory, "/var/tmp/rabbitmq-tracing"},
	    {username, <<"guest">>},
	    {password, <<"guest">>}
	  ]},
		{broker_version_requirements, []}
]}.