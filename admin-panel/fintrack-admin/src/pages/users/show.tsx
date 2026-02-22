import { useShow } from "@refinedev/core";
import { Show } from "@refinedev/antd";
import {
    Typography,
    Descriptions,
    Tag,
    Card,
    Row,
    Col,
    Statistic,
    Spin,
    Button,
    message,
} from "antd";
import { UnorderedListOutlined, BankOutlined } from "@ant-design/icons";
import { useNavigate } from "react-router";
import { useState } from "react";
import { API_URL, TOKEN_KEY } from "../../providers/constants";

const { Title } = Typography;

interface UserDetail {
    id: number;
    username: string;
    email: string;
    role: string;
    blocked: boolean;
    transactionCount: number;
    totalIncome: number;
    totalExpense: number;
}

export const UserShow = () => {
    const { query } = useShow<UserDetail>({ resource: "users" });
    const { data, isLoading } = query;
    const record = data?.data;
    const navigate = useNavigate();
    const [seLoading, setSeLoading] = useState(false);

    if (isLoading) {
        return (
            <div style={{ display: "flex", justifyContent: "center", padding: 100 }}>
                <Spin size="large" />
            </div>
        );
    }

    return (
        <Show
            title={`User: ${record?.username}`}
            headerButtons={({ defaultButtons }) => (
                <>
                    {defaultButtons}
                    <Button
                        type="primary"
                        icon={<UnorderedListOutlined />}
                        onClick={() => navigate(`/users/${record?.id}/transactions`)}
                    >
                        Transactions
                    </Button>
                    <Button
                        icon={<BankOutlined />}
                        loading={seLoading}
                        onClick={async () => {
                            setSeLoading(true);
                            try {
                                const res = await fetch(
                                    `${API_URL}/admin/users/${record?.id}/saltedge/session`,
                                    {
                                        method: "POST",
                                        headers: {
                                            Authorization: `Bearer ${localStorage.getItem(TOKEN_KEY)}`,
                                        },
                                    }
                                );
                                if (res.ok) {
                                    const json = await res.json();
                                    message.success("Session created");
                                    window.open(json.connectUrl, "_blank");
                                } else {
                                    message.error("Failed to create session");
                                }
                            } catch {
                                message.error("Error connecting to Salt Edge");
                            } finally {
                                setSeLoading(false);
                            }
                        }}
                    >
                        Link Salt Edge
                    </Button>
                </>
            )}
        >
            <Descriptions bordered column={2}>
                <Descriptions.Item label="ID">{record?.id}</Descriptions.Item>
                <Descriptions.Item label="Username">
                    {record?.username}
                </Descriptions.Item>
                <Descriptions.Item label="Email">{record?.email}</Descriptions.Item>
                <Descriptions.Item label="Role">
                    <Tag color={record?.role === "ADMIN" ? "gold" : "blue"}>
                        {record?.role}
                    </Tag>
                </Descriptions.Item>
                <Descriptions.Item label="Status">
                    <Tag color={record?.blocked ? "red" : "green"}>
                        {record?.blocked ? "Blocked" : "Active"}
                    </Tag>
                </Descriptions.Item>
            </Descriptions>

            <Title level={5} style={{ marginTop: 24, marginBottom: 16 }}>
                Transaction Summary
            </Title>
            <Row gutter={16}>
                <Col span={8}>
                    <Card bordered={false}>
                        <Statistic
                            title="Total Transactions"
                            value={record?.transactionCount ?? 0}
                        />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card bordered={false}>
                        <Statistic
                            title="Total Income"
                            value={record?.totalIncome ?? 0}
                            precision={2}
                            prefix="$"
                            valueStyle={{ color: "#3f8600" }}
                        />
                    </Card>
                </Col>
                <Col span={8}>
                    <Card bordered={false}>
                        <Statistic
                            title="Total Expense"
                            value={record?.totalExpense ?? 0}
                            precision={2}
                            prefix="$"
                            valueStyle={{ color: "#cf1322" }}
                        />
                    </Card>
                </Col>
            </Row>
        </Show>
    );
};
