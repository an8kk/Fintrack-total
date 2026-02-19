import { useState } from "react";
import { Card, Button, Typography, Spin, Empty } from "antd";
import { ExperimentOutlined, ReloadOutlined } from "@ant-design/icons";
import { API_URL, TOKEN_KEY } from "../../providers/constants";

const { Title, Paragraph } = Typography;

export const AiInsights = () => {
    const [insights, setInsights] = useState<string | null>(null);
    const [loading, setLoading] = useState(false);
    const [error, setError] = useState<string | null>(null);

    const fetchInsights = async () => {
        setLoading(true);
        setError(null);
        try {
            const res = await fetch(`${API_URL}/admin/ai/system-insights`, {
                headers: { Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}` },
            });
            if (res.ok) {
                const data = await res.json();
                setInsights(data.insights);
            } else {
                setError("Failed to fetch insights");
            }
        } catch {
            setError("Error connecting to AI service");
        } finally {
            setLoading(false);
        }
    };

    return (
        <div style={{ padding: 24 }}>
            <div style={{ display: "flex", justifyContent: "space-between", marginBottom: 16 }}>
                <Title level={3}>
                    <ExperimentOutlined /> AI System Insights
                </Title>
                <Button
                    type="primary"
                    icon={<ReloadOutlined />}
                    onClick={fetchInsights}
                    loading={loading}
                >
                    {insights ? "Refresh" : "Generate Insights"}
                </Button>
            </div>

            <Card
                bordered={false}
                style={{
                    borderRadius: 12,
                    minHeight: 300,
                    background: "linear-gradient(135deg, #1a1a2e 0%, #16213e 100%)",
                }}
            >
                {loading ? (
                    <div style={{ textAlign: "center", padding: 60 }}>
                        <Spin size="large" />
                        <Paragraph style={{ color: "#aaa", marginTop: 16 }}>
                            Analyzing system-wide transaction patterns...
                        </Paragraph>
                    </div>
                ) : error ? (
                    <Paragraph type="danger">{error}</Paragraph>
                ) : insights ? (
                    <div
                        style={{
                            whiteSpace: "pre-wrap",
                            color: "#e0e0e0",
                            fontSize: 14,
                            lineHeight: 1.8,
                        }}
                    >
                        {insights}
                    </div>
                ) : (
                    <Empty
                        description={
                            <span style={{ color: "#888" }}>
                                Click "Generate Insights" to analyze system-wide spending patterns
                            </span>
                        }
                    />
                )}
            </Card>
        </div>
    );
};
