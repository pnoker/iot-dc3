{application, 'rabbitmq_shovel', [
	{description, "Data Shovel for RabbitMQ"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['Elixir.RabbitMQ.CLI.Ctl.Commands.DeleteShovelCommand','Elixir.RabbitMQ.CLI.Ctl.Commands.RestartShovelCommand','Elixir.RabbitMQ.CLI.Ctl.Commands.ShovelStatusCommand','rabbit_amqp091_shovel','rabbit_amqp10_shovel','rabbit_log_shovel','rabbit_shovel','rabbit_shovel_behaviour','rabbit_shovel_config','rabbit_shovel_dyn_worker_sup','rabbit_shovel_dyn_worker_sup_sup','rabbit_shovel_locks','rabbit_shovel_parameters','rabbit_shovel_status','rabbit_shovel_sup','rabbit_shovel_util','rabbit_shovel_worker','rabbit_shovel_worker_sup']},
	{registered, [rabbitmq_shovel_sup]},
	{applications, [kernel,stdlib,crypto,rabbit_common,rabbit,amqp_client,amqp10_client]},
	{mod, {rabbit_shovel, []}},
	{env, [
	    {defaults, [
	        {prefetch_count,     1000},
	        {ack_mode,           on_confirm},
	        {publish_fields,     []},
	        {publish_properties, []},
	        {reconnect_delay,    5}
	      ]}
	  ]},
		{broker_version_requirements, []}
]}.