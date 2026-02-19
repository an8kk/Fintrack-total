import { useCustom } from "@refinedev/core";
import { Row, Col, Card, Statistic, Typography, Spin } from "antd";
import {
    UserOutlined,
    TransactionOutlined,
    DollarOutlined,
    ThunderboltOutlined,
} from "@ant-design/icons";
import { API_URL } from "../../providers/constants";

const { Title } = Typography;

interface AdminStats {
    totalUsers: number;
    totalTransactions: number;
    totalVolume: number;
    activeToday: number;
}

export const Dashboard = () => {
    const result = useCustom<AdminStats>({
        url: `${API_URL}/admin/stats`,
        method: "get",
    });

    const data = (result as any)?.data ?? (result as any)?.query?.data;
    const isLoading = (result as any)?.isLoading ?? (result as any)?.query?.isLoading ?? false;
    const stats = data?.data;

    if (isLoading) {
        return (
            <div style={{ display: "flex", justifyContent: "center", padding: 100 }}>
                <Spin size="large" />
            </div>
        );
    }

    return (
        <div style={{ padding: 24 }}>
            <Title level={3} style={{ marginBottom: 24 }}>
                Dashboard
            </Title>

            <Row gutter={[24, 24]}>
                <Col xs={24} sm={12} lg={6}>
                    <Card
                        bordered={false}
                        style={{
                            borderRadius: 16,
                            background: "linear-gradient(135deg, #2E7D32 0%, #43A047 100%)",
                        }}
                    >
                        <Statistic
                            title={
                                <span style={{ color: "rgba(255,255,255,0.75)" }}>
                                    Total Users
                                </span>
                            }
                            value={stats?.totalUsers ?? 0}
                            prefix={<UserOutlined />}
                            valueStyle={{ color: "#fff", fontSize: 32 }}
                        />
                    </Card>
                </Col>

                <Col xs={24} sm={12} lg={6}>
                    <Card
                        bordered={false}
                        style={{
                            borderRadius: 16,
                            background: "linear-gradient(135deg, #1565C0 0%, #42A5F5 100%)",
                        }}
                    >
                        <Statistic
                            title={
                                <span style={{ color: "rgba(255,255,255,0.75)" }}>
                                    Total Transactions
                                </span>
                            }
                            value={stats?.totalTransactions ?? 0}
                            prefix={<TransactionOutlined />}
                            valueStyle={{ color: "#fff", fontSize: 32 }}
                        />
                    </Card>
                </Col>

                <Col xs={24} sm={12} lg={6}>
                    <Card
                        bordered={false}
                        style={{
                            borderRadius: 16,
                            background: "linear-gradient(135deg, #E65100 0%, #FF9800 100%)",
                        }}
                    >
                        <Statistic
                            title={
                                <span style={{ color: "rgba(255,255,255,0.75)" }}>
                                    Total Volume
                                </span>
                            }
                            value={stats?.totalVolume ?? 0}
                            prefix={<DollarOutlined />}
                            precision={2}
                            valueStyle={{ color: "#fff", fontSize: 32 }}
                        />
                    </Card>
                </Col>

                <Col xs={24} sm={12} lg={6}>
                    <Card
                        bordered={false}
                        style={{
                            borderRadius: 16,
                            background: "linear-gradient(135deg, #6A1B9A 0%, #AB47BC 100%)",
                        }}
                    >
                        <Statistic
                            title={
                                <span style={{ color: "rgba(255,255,255,0.75)" }}>
                                    Active Today
                                </span>
                            }
                            value={stats?.activeToday ?? 0}
                            prefix={<ThunderboltOutlined />}
                            valueStyle={{ color: "#fff", fontSize: 32 }}
                        />
                    </Card>
                </Col>
            </Row>
        </div>
    );
};
