{application, 'rabbit_common', [
	{description, "Modules shared by rabbitmq-server and rabbitmq-erlang-client"},
	{vsn, "3.9.11"},
	{id, "v3.9.10-16-g44036a2"},
	{modules, ['app_utils','code_version','credit_flow','delegate','delegate_sup','file_handle_cache','file_handle_cache_stats','gen_server2','mirrored_supervisor','mirrored_supervisor_locks','mnesia_sync','pmon','priority_queue','rabbit_amqp_connection','rabbit_amqqueue_common','rabbit_auth_backend_dummy','rabbit_auth_mechanism','rabbit_authn_backend','rabbit_authz_backend','rabbit_basic_common','rabbit_binary_generator','rabbit_binary_parser','rabbit_cert_info','rabbit_channel_common','rabbit_command_assembler','rabbit_control_misc','rabbit_core_metrics','rabbit_data_coercion','rabbit_env','rabbit_error_logger_handler','rabbit_event','rabbit_exchange_type','rabbit_framing_amqp_0_8','rabbit_framing_amqp_0_9_1','rabbit_heartbeat','rabbit_http_util','rabbit_json','rabbit_log','rabbit_misc','rabbit_msg_store_index','rabbit_net','rabbit_nodes_common','rabbit_numerical','rabbit_password_hashing','rabbit_pbe','rabbit_peer_discovery_backend','rabbit_policy_validator','rabbit_queue_collector','rabbit_registry','rabbit_registry_class','rabbit_resource_monitor_misc','rabbit_runtime','rabbit_runtime_parameter','rabbit_semver','rabbit_semver_parser','rabbit_ssl_options','rabbit_types','rabbit_writer','supervisor2','vm_memory_monitor','worker_pool','worker_pool_sup','worker_pool_worker']},
	{registered, []},
	{applications, [kernel,stdlib,compiler,crypto,public_key,sasl,ssl,syntax_tools,tools,xmerl,jsx,recon,credentials_obfuscation]},
	{env, []},
	%% Hex.pm package informations.
	{licenses, ["MPL-2.0"]},
	{links, [
	    {"Website", "https://www.rabbitmq.com/"},
	    {"GitHub", "https://github.com/rabbitmq/rabbitmq-common"}
	  ]},
	{build_tools, ["make", "rebar3"]},
	{files, [
	    	    "erlang.mk",
	    "git-revisions.txt",
	    "include",
	    "LICENSE*",
	    "Makefile",
	    "rabbitmq-components.mk",
	    "README",
	    "README.md",
	    "src",
	    "mk"
	  ]}
]}.