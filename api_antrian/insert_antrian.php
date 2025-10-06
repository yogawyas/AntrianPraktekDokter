<?php
include "db.php";

$user_email = $_POST['email'];
$nama = $_POST['nama'];
$usia = $_POST['usia'];
$tanggal = $_POST['tanggal'];
$jam = $_POST['jam'];
$dokter = $_POST['dokter'];
$keluhan = $_POST['keluhan'];

$sql = "INSERT INTO antrian (user_email, nama_pasien, usia, tanggal, jam, dokter, keluhan)
        VALUES ('$user_email', '$nama', '$usia', '$tanggal', '$jam', '$dokter', '$keluhan')";

if ($conn->query($sql) === TRUE) {
    echo json_encode(["success" => true, "message" => "Antrian berhasil disimpan"]);
} else {
    echo json_encode(["success" => false, "message" => $conn->error]);
}
$conn->close();
?>
