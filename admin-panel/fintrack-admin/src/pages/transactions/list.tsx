import { useParams, useNavigate } from "react-router";
import { useCustom } from "@refinedev/core";
import { List } from "@refinedev/antd";
import { Table, Tag, Button, Space, Typography } from "antd";
import { PlusOutlined, ArrowLeftOutlined } from "@ant-design/icons";
import { API_URL } from "../../providers/constants";

const { Text } = Typography;

interface TransactionRecord {
    id: number;
    amount: number;
    category: string;
    description: string;
    date: string;
    type: string;
    currency: string;
}

export const TransactionList = () => {
    const { userId } = useParams<{ userId: string }>();
    const navigate = useNavigate();

    const result = useCustom<TransactionRecord[]>({
        url: `${API_URL}/admin/users/${userId}/transactions`,
        method: "get",
    });

    const data = (result as any)?.data?.data ?? (result as any)?.query?.data?.data ?? [];
    const isLoading = (result as any)?.isLoading ?? (result as any)?.query?.isLoading ?? false;

    return (
        <List
            title={`Transactions — User #${userId}`}
            headerButtons={
                <Space>
                    <Button
                        icon={<ArrowLeftOutlined />}
                        onClick={() => navigate(`/users/show/${userId}`)}
                    >
                        Back
                    </Button>
                    <Button
                        type="primary"
                        icon={<PlusOutlined />}
                        onClick={() => navigate(`/users/${userId}/transactions/create`)}
                    >
                        Create Transaction
                    </Button>
                </Space>
            }
        >
            <Table dataSource={data} loading={isLoading} rowKey="id">
                <Table.Column dataIndex="id" title="ID" width={80} />
                <Table.Column
                    dataIndex="amount"
                    title="Amount"
                    render={(val: number, record: TransactionRecord) => (
                        <Text
                            strong
                            style={{
                                color: record.type === "INCOME" ? "#3f8600" : "#cf1322",
                            }}
                        >
                            {record.type === "INCOME" ? "+" : "-"}
                            {val?.toFixed(2)} {record.currency}
                        </Text>
                    )}
                />
                <Table.Column dataIndex="category" title="Category" />
                <Table.Column dataIndex="description" title="Description" />
                <Table.Column
                    dataIndex="type"
                    title="Type"
                    render={(val: string) => (
                        <Tag color={val === "INCOME" ? "green" : "red"}>{val}</Tag>
                    )}
                    width={100}
                />
                <Table.Column
                    dataIndex="date"
                    title="Date"
                    render={(val: string) =>
                        val ? new Date(val).toLocaleDateString() : "—"
                    }
                    width={120}
                />
            </Table>
        </List>
    );
};
