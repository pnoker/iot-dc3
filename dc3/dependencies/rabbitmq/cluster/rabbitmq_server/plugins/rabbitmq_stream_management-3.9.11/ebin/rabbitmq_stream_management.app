{application, 'rabbitmq_stream_management', [
	{description, "RabbitMQ Stream Management"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['rabbit_stream_connection_consumers_mgmt','rabbit_stream_connection_mgmt','rabbit_stream_connection_publishers_mgmt','rabbit_stream_connections_mgmt','rabbit_stream_connections_vhost_mgmt','rabbit_stream_consumers_mgmt','rabbit_stream_management_utils','rabbit_stream_mgmt_db','rabbit_stream_publishers_mgmt']},
	{registered, []},
	{applications, [kernel,stdlib,rabbit,rabbitmq_management,rabbitmq_stream]},
	{env, [
]}
]}.