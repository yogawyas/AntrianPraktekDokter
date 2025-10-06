<?php
header('Content-Type: application/json');
include "db.php";

$nama = $_POST['nama'] ?? '';
$email = $_POST['email'] ?? '';
$password = $_POST['password'] ?? '';

if ($nama == '' || $email == '' || $password == '') {
    echo json_encode(["success" => false, "message" => "Data tidak lengkap"]);
    exit;
}

$hashed = password_hash($password, PASSWORD_DEFAULT);

$sql = "INSERT INTO users (nama, email, password) VALUES ('$nama','$email','$hashed')";

if ($conn->query($sql) === TRUE) {
    echo json_encode(["success" => true, "message" => "Register berhasil"]);
} else {
    echo json_encode(["success" => false, "message" => "Email sudah digunakan"]);
}
?>
