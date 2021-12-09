{application, 'rabbitmq_prelaunch', [
	{description, "RabbitMQ prelaunch setup"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['rabbit_boot_state','rabbit_boot_state_sup','rabbit_boot_state_systemd','rabbit_logger_fmt_helpers','rabbit_logger_json_fmt','rabbit_logger_std_h','rabbit_logger_text_fmt','rabbit_prelaunch','rabbit_prelaunch_app','rabbit_prelaunch_conf','rabbit_prelaunch_dist','rabbit_prelaunch_early_logging','rabbit_prelaunch_erlang_compat','rabbit_prelaunch_errors','rabbit_prelaunch_sighandler','rabbit_prelaunch_sup']},
	{registered, [rabbitmq_prelaunch_sup]},
	{applications, [kernel,stdlib,rabbit_common,cuttlefish,jsx]},
	{mod, {rabbit_prelaunch_app, []}},
	{env, []}
]}.