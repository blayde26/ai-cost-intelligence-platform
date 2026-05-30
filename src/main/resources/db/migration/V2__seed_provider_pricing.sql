INSERT INTO provider_pricing (
    provider,
    model,
    input_cost_per_million,
    output_cost_per_million,
    effective_date
) VALUES
    ('OPENAI', 'gpt-4o-mini', 0.150000, 0.600000, DATE '2024-07-18'),
    ('OPENAI', 'gpt-4o', 5.000000, 15.000000, DATE '2024-05-13'),
    ('MOCK_LLM', 'mock-gpt-4o-mini', 0.010000, 0.030000, DATE '2026-01-01'),
    ('OLLAMA', 'llama3.2', 0.000000, 0.000000, DATE '2026-01-01')
ON CONFLICT DO NOTHING;
